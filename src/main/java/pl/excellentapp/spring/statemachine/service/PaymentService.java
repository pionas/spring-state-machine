package pl.excellentapp.spring.statemachine.service;

import org.springframework.statemachine.StateMachine;
import pl.excellentapp.spring.statemachine.domain.Payment;
import pl.excellentapp.spring.statemachine.domain.PaymentEvent;
import pl.excellentapp.spring.statemachine.domain.PaymentState;

public interface PaymentService {

    Payment newPayment(Payment payment);

    StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId);

    StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId);

    StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId);
}