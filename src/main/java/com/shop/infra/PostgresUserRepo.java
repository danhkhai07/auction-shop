package com.shop.infra;

import com.shop.application.UserRepository;
import com.shop.db.DB;
import com.shop.domain.Role;
import com.shop.domain.User;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

@Repository
public class PostgresUserRepo {

}
