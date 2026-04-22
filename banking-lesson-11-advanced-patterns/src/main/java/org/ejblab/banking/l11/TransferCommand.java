package org.ejblab.banking.l11;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Input DTO with Bean Validation constraints. When passed to a method on
 * a bean that is validated (see {@link AdjustmentService}), Jakarta EE
 * triggers validation automatically and throws
 * {@link jakarta.validation.ConstraintViolationException} on violations —
 * which rolls back the surrounding transaction.
 */
public record TransferCommand(
        @NotBlank @Size(max = 34) String fromAccount,
        @NotBlank @Size(max = 34) String toAccount,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount
) implements Serializable { }
