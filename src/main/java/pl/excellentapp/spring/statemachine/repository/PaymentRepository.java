package pl.excellentapp.spring.statemachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.excellentapp.spring.statemachine.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}