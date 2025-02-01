package pl.excellentapp.spring.statemachine.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import pl.excellentapp.spring.statemachine.AbstractTestIT;
import pl.excellentapp.spring.statemachine.domain.PaymentEvent;
import pl.excellentapp.spring.statemachine.domain.PaymentState;
import pl.excellentapp.spring.statemachine.service.PaymentServiceImpl;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class StateMachineConfigurationIT extends AbstractTestIT {

    @Autowired
    private StateMachineFactory<PaymentState, PaymentEvent> factory;

    private StateMachine<PaymentState, PaymentEvent> stateMachine;

    @BeforeEach
    public void setUp() {
        stateMachine = factory.getStateMachine();
        stateMachine.startReactively().block();
    }

    @Test
    public void testInitialState() {
        // given

        // when

        // then
        assertThat(stateMachine.getState().getId()).isEqualTo(PaymentState.NEW);
    }

    @Test
    public void testPreAuthorizationApproved() {
        // given

        // when
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTHORIZE))).blockLast();

        // then
        assertThat(stateMachine.getState().getId()).isIn(PaymentState.PRE_AUTH, PaymentState.PRE_AUTH_ERROR);
    }

    @Test
    public void testPreAuthorizationDeclined() {
        // given

        // when
        for (int i = 0; i < 100; i++) {
            stateMachine = factory.getStateMachine();
            stateMachine.startReactively().block();
            stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTHORIZE))).blockLast();

            // then
            if (stateMachine.getState().getId() == PaymentState.PRE_AUTH_ERROR) {
                return;
            }
        }

        // then
        fail("Nie udało się uzyskać przejścia do PRE_AUTH_ERROR w 100 próbach");
    }

    @Test
    public void testFullAuthorizationFlow() {
        // given
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTHORIZE))).blockLast();

        // when
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.AUTHORIZE))).blockLast();

        // then
        assertThat(stateMachine.getState().getId()).isIn(PaymentState.PRE_AUTH, PaymentState.PRE_AUTH_ERROR);
    }

    @Test
    void testNewStateMachine() {
        // given

        // when
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTHORIZE))).blockLast();
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTH_APPROVED))).blockLast();
        stateMachine.sendEvent(Mono.just(getPaymentEventMessage(PaymentEvent.PRE_AUTH_DECLINED))).blockLast();

        // then
        assertThat(stateMachine.getState().getId()).isEqualTo(PaymentState.PRE_AUTH_ERROR);
    }

    private Message<PaymentEvent> getPaymentEventMessage(PaymentEvent preAuthorize) {
        return MessageBuilder.withPayload(preAuthorize)
                .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, "headerPaymentId")
                .build();
    }
}