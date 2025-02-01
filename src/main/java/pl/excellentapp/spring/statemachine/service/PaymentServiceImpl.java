package pl.excellentapp.spring.statemachine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.excellentapp.spring.statemachine.domain.Payment;
import pl.excellentapp.spring.statemachine.domain.PaymentEvent;
import pl.excellentapp.spring.statemachine.domain.PaymentState;
import pl.excellentapp.spring.statemachine.repository.PaymentRepository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_ID_HEADER = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;

    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        final var sm = build(paymentId);
        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);
        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        final var sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTH_APPROVED);

        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        final var sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTH_DECLINED);

        return sm;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event) {
        final var paymentEventMessage = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_ID_HEADER, paymentId)
                .build();

        sm.sendEvent(Mono.just(paymentEventMessage)).blockLast();
    }

    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
        final var payment = paymentRepository.getReferenceById(paymentId);
        final var stateMachine = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

        return stateMachine.stopReactively()
                .then(Mono.defer(() -> {
                    stateMachine.getStateMachineAccessor()
                            .doWithAllRegions(paymentStatePaymentEventStateMachineAccess -> {
                                paymentStatePaymentEventStateMachineAccess.resetStateMachineReactively(new DefaultStateMachineContext<>(payment.getState(), null, null, null))
                                        .subscribe();
                            });
                    return Mono.empty();
                }))
                .then(stateMachine.startReactively())
                .thenReturn(stateMachine)
                .block();
    }
}