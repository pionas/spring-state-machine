package pl.excellentapp.spring.statemachine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import pl.excellentapp.spring.statemachine.domain.PaymentEvent;
import pl.excellentapp.spring.statemachine.domain.PaymentState;
import pl.excellentapp.spring.statemachine.service.PaymentServiceImpl;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Random;

@Slf4j
@EnableStateMachineFactory
@Configuration(proxyBeanMethods = false)
public class StateMachineConfiguration extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH)
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        transitions.withExternal().guard(guard())
                .source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE)
                .action(preAuthAction())
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info("stateChanged(from: {}, to: {})", from, to);
            }
        };
        config.withConfiguration()
                .listener(adapter);
    }

    private Action<PaymentState, PaymentEvent> preAuthAction() {
        return context -> {
            log.info("PreAuth was called!!!");
            final var event = new Random().nextInt(10) < 8 ? PaymentEvent.PRE_AUTH_APPROVED : PaymentEvent.PRE_AUTH_DECLINED;
            log.info(event == PaymentEvent.PRE_AUTH_APPROVED ? "Approved" : "Declined! No Credit!!!!!!");
            context.getStateMachine().sendEvent(getMessageMono(context, event))
                    .blockLast();
        };
    }

    private Mono<Message<PaymentEvent>> getMessageMono(StateContext<PaymentState, PaymentEvent> context, PaymentEvent event) {
        return Mono.just(getPaymentEventMessage(context, event));
    }

    private Message<PaymentEvent> getPaymentEventMessage(StateContext<PaymentState, PaymentEvent> context, PaymentEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                .build();
    }

    private Guard<PaymentState, PaymentEvent> guard() {
        return context -> context.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER) != null;
    }
}