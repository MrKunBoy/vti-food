package VTIFOOD.VTI_Food.service.serviceImpl;

import VTIFOOD.VTI_Food.DTO.OrderDto;
import VTIFOOD.VTI_Food.exception.ResourceNotFoundException;
import VTIFOOD.VTI_Food.form.OrderCreateForm;
import VTIFOOD.VTI_Food.form.OrderUpdateForm;
import VTIFOOD.VTI_Food.mapper.OrderMapper;
import VTIFOOD.VTI_Food.model.Order;
import VTIFOOD.VTI_Food.model.PaymentMethod;
import VTIFOOD.VTI_Food.repository.OrderRepository;
import VTIFOOD.VTI_Food.repository.PaymentMethodRepository;
import VTIFOOD.VTI_Food.service.entityservice.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    public void createOrderFromCart(OrderCreateForm form) {
        try {
            Boolean paymentStatus = form.getPaymentStatus();

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("CreateOrder");
            query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
            query.setParameter(1, form.getUserId());

            LocalDateTime deliveryDate = now().plusMinutes(30);

            if (form.getDeliveryDate() != null) {
                deliveryDate = form.getDeliveryDate();
            }
            query.registerStoredProcedureParameter(2, LocalDateTime.class, ParameterMode.IN);
            query.setParameter(2, deliveryDate);

            query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            query.setParameter(3, form.getDeliveryAddress());

            query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            query.setParameter(4, Order.OrderStatus.XAC_NHAN.getValue());

            query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            query.setParameter(5, form.getNote());

            query.registerStoredProcedureParameter(6, Long.class, ParameterMode.IN);
            query.setParameter(6, form.getPaymentMethodId());

            query.registerStoredProcedureParameter(7, Boolean.class, ParameterMode.IN);
            query.setParameter(7, form.getPaymentStatus());
            LocalDateTime paymentDate = null;
            if (paymentStatus) {
                paymentDate = now();
            }
            query.registerStoredProcedureParameter(8, LocalDateTime.class, ParameterMode.IN);
            query.setParameter(8, paymentDate);

//            query.registerStoredProcedureParameter(9, BigDecimal.class, ParameterMode.IN);
//            query.setParameter(9, form.getTotalAmount());

            query.execute();
        } catch (Exception e) {
            throw new RuntimeException("Đặt hàng thất bại " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(OrderMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderMapper::map)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    @Override
    public Order updateOrder(Long id, OrderUpdateForm form) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        order.setDeliveryDate(form.getDeliveryDate());
        order.setDeliveryAddress(form.getDeliveryAddress());
        order.setOrderStatus(form.getOrderStatus());
        order.setNote(form.getNote());
        order.setPaymentStatus(form.getPaymentStatus());
        order.setPaymentDate(form.getPaymentDate());

        PaymentMethod paymentMethod = paymentMethodRepository.findById(form.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương thức thanh toán"));
        order.setPaymentMethod(paymentMethod);
        return orderRepository.save(order);
    }
}
