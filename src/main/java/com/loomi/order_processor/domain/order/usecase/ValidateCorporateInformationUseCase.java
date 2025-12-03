package com.loomi.order_processor.domain.order.usecase;

public interface ValidateCorporateInformationUseCase {
    boolean isCnpjValid(String cnpj);

    boolean isInscricaoEstadualValid(String ie);
}
