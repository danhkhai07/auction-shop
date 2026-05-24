package com.shop.infra;

import com.shop.application.ItemRepository;
import com.shop.domain.Item;
import com.shop.domain.User;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PostgresItemRepo implements ItemRepository {

    // su dung database client de tuong tac voi database
    private final DatabaseClient databaseClient;

    // Constructor injection
    public PostgresItemRepo(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Boolean> existsByID(String id) {
        // kiem tra xem id co rong khong
        if (!StringUtils.hasText(id)) {
            return Mono.just(false);
        }

        // truy van xem item co ton tai khong
        String sql = "SELECT EXISTS (SELECT 1 FROM items WHERE id = :id) AS item_exists";
        return databaseClient.sql(sql)
                .bind("id", id)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("item_exists", Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<Item> getAll() {
        String sql = "SELECT i.id AS item_id, i.name, i.description, u.id AS seller_id, u.username AS seller_name " +
                "FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "ORDER BY i.id";

        return databaseClient.sql(sql)
                .map((row, metadata) -> {
                    User seller = new User(
                            row.get("seller_id", String.class),
                            row.get("seller_name", String.class),
                            "");

                    return new Item(
                            row.get("item_id", String.class),
                            row.get("name", String.class),
                            row.get("description", String.class),
                            seller);
                })
                .all();
    }

    @Override
    public Mono<Item> getByID(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        // Lay thong tin item va ket noi voi bang users de lay thong tin nguoi ban
        // (seller)
        String sql = "SELECT i.id AS item_id, i.name, i.description, u.id AS seller_id, u.username AS seller_name " +
                "FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.id = :id";

        return databaseClient.sql(sql)
                .bind("id", id)
                .map((row, metadata) -> {
                    // tao doi tuong user dong vai tro la seller
                    User seller = new User(
                            row.get("seller_id", String.class),
                            row.get("seller_name", String.class),
                            "");

                    // tra ve doi tuong item
                    return new Item(
                            row.get("item_id", String.class),
                            row.get("name", String.class),
                            row.get("description", String.class),
                            seller);
                })
                .one();

    }

    @Override
    public Mono<Item> getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Mono.empty();
        }

        // tuong tu ham getByID nhung tim theo ten
        String sql = "SELECT i.id AS item_id, i.name, i.description, u.id AS seller_id, u.username AS seller_name " +
                "FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.name = :name";

        return databaseClient.sql(sql)
                .bind("name", name)
                .map((row, metadata) -> {
                    User seller = new User(
                            row.get("seller_id", String.class),
                            row.get("seller_name", String.class),
                            "");

                    return new Item(
                            row.get("item_id", String.class),
                            row.get("name", String.class),
                            row.get("description", String.class),
                            seller);
                })
                .one();

    }

    @Override
    public Mono<Void> saveItem(Item item) {
        // cap nhat thong tin item da co
        if (item == null || !StringUtils.hasText(item.getId())) {
            return Mono.error(new IllegalArgumentException("Item không hợp lệ"));
        }

        String sql = "UPDATE items SET name = :name, description = :description, seller_id = :sellerId WHERE id = :id";

        return databaseClient.sql(sql)
                .bind("id", item.getId())
                .bind("name", item.getName())
                // xu ly truong hop description bi null
                .bind("description", item.getDescription() != null ? item.getDescription() : "")
                .bind("sellerId", item.getSeller().getId())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> deleteByID(String id) {
        // kiem tra xem id co rong khong
        if (!StringUtils.hasText(id)) {
            return Mono.empty();
        }

        // xoa item theo id
        String sql = "DELETE FROM items WHERE id = :id";
        return databaseClient.sql(sql)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then();

    }

    @Override
    public Mono<Void> newItem(Item item) {
        // them item moi vao database
        if (item == null || !StringUtils.hasText(item.getId())) {
            return Mono.error(new IllegalArgumentException("Item không hợp lệ"));
        }

        String sql = "INSERT INTO items (id, name, description, seller_id) VALUES (:id, :name, :description, :sellerId)";

        return databaseClient.sql(sql)
                .bind("id", item.getId())
                .bind("name", item.getName())
                // xu ly truong hop description bi null
                .bind("description", item.getDescription() != null ? item.getDescription() : "")
                .bind("sellerId", item.getSeller().getId())
                .fetch()
                .rowsUpdated()
                .then();

    }
}
