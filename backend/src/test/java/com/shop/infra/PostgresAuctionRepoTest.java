package com.shop.infra;

import com.shop.domain.Auction;
import com.shop.domain.Item;
import com.shop.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresAuctionRepoTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void existsByIdQueriesDatabaseForNonBlankId() {
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec spec = mock(DatabaseClient.GenericExecuteSpec.class);
        RowsFetchSpec<Boolean> fetchSpec = mock(RowsFetchSpec.class);
        when(databaseClient.sql(contains("SELECT EXISTS"))).thenReturn(spec);
        when(spec.bind("id", "auction-1")).thenReturn(spec);
        when(spec.map(any(BiFunction.class))).thenReturn(fetchSpec);
        when(fetchSpec.one()).thenReturn(Mono.just(true));

        PostgresAuctionRepo repo = new PostgresAuctionRepo(databaseClient);

        StepVerifier.create(repo.existsByID("auction-1"))
                .expectNext(true)
                .verifyComplete();

        verify(databaseClient).sql(contains("WHERE id = :id"));
    }

    @Test
    void existsByIdDoesNotQueryDatabaseForBlankId() {
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        PostgresAuctionRepo repo = new PostgresAuctionRepo(databaseClient);

        StepVerifier.create(repo.existsByID(" "))
                .expectNext(false)
                .verifyComplete();

        verify(databaseClient, times(0)).sql(any(String.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void getAllLoadsBidsWithOneBatchQuery() {
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec auctionsSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        RowsFetchSpec<Auction> auctionsFetchSpec = mock(RowsFetchSpec.class);
        DatabaseClient.GenericExecuteSpec bidsSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        RowsFetchSpec<PostgresAuctionRepo.AuctionBidRow> bidsFetchSpec = mock(RowsFetchSpec.class);

        Auction firstAuction = auction("auction-1");
        Auction secondAuction = auction("auction-2");

        when(databaseClient.sql(contains("ORDER BY a.start_time DESC"))).thenReturn(auctionsSpec);
        when(auctionsSpec.map(any(BiFunction.class))).thenReturn(auctionsFetchSpec);
        when(auctionsFetchSpec.all()).thenReturn(Flux.just(firstAuction, secondAuction));
        when(databaseClient.sql(contains("WHERE b.auction_id IN"))).thenReturn(bidsSpec);
        when(bidsSpec.bind(eq("auctionIds"), any())).thenReturn(bidsSpec);
        when(bidsSpec.map(any(BiFunction.class))).thenReturn(bidsFetchSpec);
        when(bidsFetchSpec.all()).thenReturn(Flux.empty());

        PostgresAuctionRepo repo = new PostgresAuctionRepo(databaseClient);

        StepVerifier.create(repo.getAll().collectList())
                .assertNext(auctions -> assertThat(auctions).extracting(Auction::getId)
                        .containsExactly("auction-1", "auction-2"))
                .verifyComplete();

        verify(databaseClient, times(1)).sql(contains("ORDER BY a.start_time DESC"));
        verify(databaseClient, times(1)).sql(contains("WHERE b.auction_id IN"));
    }

    private static Auction auction(String id) {
        User seller = new User("seller-" + id, "seller-" + id, "password");
        Item item = new Item("item-" + id, "item-" + id, "desc", seller);
        return new Auction(
                id,
                item,
                BigDecimal.TEN,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(1)
        );
    }
}
