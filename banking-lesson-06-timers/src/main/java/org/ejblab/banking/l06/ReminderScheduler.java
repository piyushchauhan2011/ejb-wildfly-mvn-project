package org.ejblab.banking.l06;

import jakarta.annotation.Resource;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Programmatic / dynamic timers via {@link TimerService}.
 *
 * <p>Unlike {@code @Schedule} which is fixed at compile time, this is how
 * you create timers at runtime (e.g. when a user creates a recurring
 * reminder or when a workflow schedules a retry).
 *
 * <p>Two common API shapes:
 * <ul>
 *   <li>{@link TimerService#createSingleActionTimer(long, TimerConfig)}
 *       - fires once after a delay.</li>
 *   <li>{@link TimerService#createCalendarTimer(ScheduleExpression, TimerConfig)}
 *       - recurring, cron-like.</li>
 * </ul>
 *
 * <p>The info payload must be {@link Serializable} if the timer is
 * {@linkplain TimerConfig#setPersistent persistent} (the default).
 */
@Singleton
public class ReminderScheduler {

    private static final Logger log = Logger.getLogger(ReminderScheduler.class.getName());

    @Resource
    TimerService timers;

    public record ReminderInfo(Long reminderId, String message) implements Serializable {}

    public long scheduleOnce(String message, Duration delay) {
        TimerConfig cfg = new TimerConfig();
        cfg.setInfo(new ReminderInfo(nextId(), message));
        cfg.setPersistent(true);
        Timer t = timers.createSingleActionTimer(delay.toMillis(), cfg);
        log.info("scheduled single-action timer fires at " + t.getNextTimeout());
        return ((ReminderInfo) t.getInfo()).reminderId();
    }

    public long scheduleDaily(String message, int hour, int minute) {
        TimerConfig cfg = new TimerConfig();
        cfg.setInfo(new ReminderInfo(nextId(), message));
        cfg.setPersistent(true);
        ScheduleExpression ex = new ScheduleExpression()
                .dayOfWeek("*")
                .hour(String.valueOf(hour))
                .minute(String.valueOf(minute))
                .second("0");
        Timer t = timers.createCalendarTimer(ex, cfg);
        log.info("scheduled daily timer next=" + t.getNextTimeout());
        return ((ReminderInfo) t.getInfo()).reminderId();
    }

    /** Cancel by reminder id carried in the TimerConfig info. */
    public boolean cancel(long reminderId) {
        for (Timer t : timers.getTimers()) {
            Serializable info = t.getInfo();
            if (info instanceof ReminderInfo ri && ri.reminderId() == reminderId) {
                t.cancel();
                return true;
            }
        }
        return false;
    }

    public List<Long> listActive() {
        Collection<Timer> active = timers.getTimers();
        var out = new ArrayList<Long>();
        for (Timer t : active) {
            if (t.getInfo() instanceof ReminderInfo ri) out.add(ri.reminderId());
        }
        return out;
    }

    /**
     * Single {@link Timeout} callback covers BOTH single-action AND calendar
     * timers created through this bean. Distinguish via {@link Timer#getSchedule()}
     * if you need to.
     *
     * <p>Default TX attribute for timeouts is REQUIRED. Any runtime exception
     * rolls back; for persistent timers, the container re-tries once by default.
     */
    @Timeout
    public void onFire(Timer t) {
        if (t.getInfo() instanceof ReminderInfo ri) {
            log.info("[reminder " + ri.reminderId() + " @ " + Instant.now() + "] " + ri.message());
        }
    }

    private long nextId() { return System.nanoTime(); }
}
