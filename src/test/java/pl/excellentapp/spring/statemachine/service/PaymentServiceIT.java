package pl.excellentapp.spring.statemachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pl.excellentapp.spring.statemachine.AbstractTestIT;
import pl.excellentapp.spring.statemachine.domain.Payment;
import pl.excellentapp.spring.statemachine.domain.PaymentState;
import pl.excellentapp.spring.statemachine.repository.PaymentRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceIT extends AbstractTestIT {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder().amount(new BigDecimal("12.99")).build();
    }

    @Transactional
    @Test
    void preAuth() {
        // given
        final var savedPayment = paymentService.newPayment(payment);

        // when
        paymentService.preAuth(savedPayment.getId());

        // then
        final var preAuthedPayment = paymentRepository.getReferenceById(savedPayment.getId());

        assertThat(preAuthedPayment.getState()).isIn(PaymentState.PRE_AUTH, PaymentState.PRE_AUTH_ERROR);
    }

    @Transactional
    @Test
    void authorizePayment() {
        // given
        final var savedPayment = paymentService.newPayment(payment);

        // when
        paymentService.authorizePayment(savedPayment.getId());

        // then
        final var preAuthedPayment = paymentRepository.getReferenceById(savedPayment.getId());

        assertThat(preAuthedPayment.getState()).isEqualTo(PaymentState.PRE_AUTH);
    }

    @Transactional
    @Test
    void declineAuth() {
        // given
        final var savedPayment = paymentService.newPayment(payment);

        // when
        paymentService.declineAuth(savedPayment.getId());

        // then
        final var preAuthedPayment = paymentRepository.getReferenceById(savedPayment.getId());

        assertThat(preAuthedPayment.getState()).isEqualTo(PaymentState.PRE_AUTH_ERROR);
    }

}