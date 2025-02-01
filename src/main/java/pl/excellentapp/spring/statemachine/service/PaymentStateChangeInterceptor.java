package pl.excellentapp.spring.statemachine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import pl.excellentapp.spring.statemachine.domain.Payment;
import pl.excellentapp.spring.statemachine.domain.PaymentEvent;
import pl.excellentapp.spring.statemachine.domain.PaymentState;
import pl.excellentapp.spring.statemachine.repository.PaymentRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PaymentStateChangeInterceptor extends StateMachineInterceptorAdapter<PaymentState, PaymentEvent> {

    private final PaymentRepository paymentRepository;

    @Override
    public void preStateChange(State<PaymentState, PaymentEvent> state, Message<PaymentEvent> message, Transition<PaymentState, PaymentEvent> transition, StateMachine<PaymentState, PaymentEvent> stateMachine, StateMachine<PaymentState, PaymentEvent> rootStateMachine) {
        Optional.ofNullable(message)
                .map(Message::getHeaders)
                .filter(headers -> headers.containsKey(PaymentServiceImpl.PAYMENT_ID_HEADER))
                .map(messageHeaders -> messageHeaders.get(PaymentServiceImpl.PAYMENT_ID_HEADER, Long.class))
                .ifPresent(paymentId -> {
                    Payment payment = paymentRepository.getReferenceById(paymentId);
                    payment.setState(state.getId());
                    paymentRepository.save(payment);
                });
    }
}