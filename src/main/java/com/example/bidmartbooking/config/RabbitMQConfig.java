package com.example.bidmartbooking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AUCTION_EVENTS = "auction.events";
    public static final String EXCHANGE_WALLET_EVENTS = "wallet.events";

    public static final String RK_WINNER_DETERMINED = "auction.event.winner-determined";
    public static final String RK_AUCTION_CLOSED = "auction.event.auction-closed";
    public static final String RK_BID_PLACED = "auction.event.bid-placed";
    public static final String RK_BALANCE_CONVERTED = "wallet.event.balance-converted";
    public static final String RK_BALANCE_RELEASED = "wallet.event.balance-released";

    public static final String QUEUE_WINNER_DETERMINED = "booking.winner-determined";
    public static final String QUEUE_AUCTION_CLOSED = "booking.auction-closed";
    public static final String QUEUE_BID_PLACED = "booking.bid-placed";
    public static final String QUEUE_BALANCE_CONVERTED = "booking.balance-converted";
    public static final String QUEUE_BALANCE_RELEASED = "booking.balance-released";

    @Bean
    TopicExchange auctionEventsExchange() {
        return new TopicExchange(EXCHANGE_AUCTION_EVENTS, true, false);
    }

    @Bean
    TopicExchange walletEventsExchange() {
        return new TopicExchange(EXCHANGE_WALLET_EVENTS, true, false);
    }

    @Bean
    Queue winnerDeterminedQueue() {
        return QueueBuilder.durable(QUEUE_WINNER_DETERMINED).build();
    }

    @Bean
    Queue auctionClosedQueue() {
        return QueueBuilder.durable(QUEUE_AUCTION_CLOSED).build();
    }

    @Bean
    Queue bidPlacedQueue() {
        return QueueBuilder.durable(QUEUE_BID_PLACED).build();
    }

    @Bean
    Queue balanceConvertedQueue() {
        return QueueBuilder.durable(QUEUE_BALANCE_CONVERTED).build();
    }

    @Bean
    Queue balanceReleasedQueue() {
        return QueueBuilder.durable(QUEUE_BALANCE_RELEASED).build();
    }

    @Bean
    Binding winnerDeterminedBinding() {
        return BindingBuilder.bind(winnerDeterminedQueue())
                .to(auctionEventsExchange())
                .with(RK_WINNER_DETERMINED);
    }

    @Bean
    Binding auctionClosedBinding() {
        return BindingBuilder.bind(auctionClosedQueue())
                .to(auctionEventsExchange())
                .with(RK_AUCTION_CLOSED);
    }

    @Bean
    Binding bidPlacedBinding() {
        return BindingBuilder.bind(bidPlacedQueue())
                .to(auctionEventsExchange())
                .with(RK_BID_PLACED);
    }

    @Bean
    Binding balanceConvertedBinding() {
        return BindingBuilder.bind(balanceConvertedQueue())
                .to(walletEventsExchange())
                .with(RK_BALANCE_CONVERTED);
    }

    @Bean
    Binding balanceReleasedBinding() {
        return BindingBuilder.bind(balanceReleasedQueue())
                .to(walletEventsExchange())
                .with(RK_BALANCE_RELEASED);
    }
}
