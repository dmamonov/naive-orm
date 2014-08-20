package org.naive.orm;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author dmitry.mamonov
 *         Created: 2014-08-20 8:14 PM
 */
public final class NaiveOrm {
    private final JdbcTemplate sql;

    public NaiveOrm(final JdbcTemplate sql) {
        this.sql = checkNotNull(sql, "sql required");
    }

    <D extends NaiveDomain> D find(final Class<D> domainType, final D filter) {
        final List<D> resultList = list(domainType, filter);
        checkState(resultList.size()<=1, "TODO explain problem");
        if (resultList.size()==1) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    <D extends NaiveDomain> D get(final Class<D> domainType, final D filter) {
        return checkNotNull(find(domainType, filter));
    }

    <D extends NaiveDomain> List<D> list(final Class<D> domainType, final D filter) {
        return list(domainType, filter, null);
    }

    <D extends NaiveDomain> List<D> list(final Class<D> domainType, final D filter, final D order) {
        checkNotNull(domainType, "domainType required");
        checkNotNull(filter, "filter required");
        final ImmutableMap<String, Field> fields = filter.getFields();
        final StringBuilder query = new StringBuilder("SELECT\n  ");
        query.append(Joiner.on(",\n  ").join(fields.keySet()));
        query.append("\nFROM ").append(domainType.getSimpleName()).append("\n");

        final ImmutableMap<String, Object> filterValues = filter.listFilters();
        query.append("WHERE ");
        query.append(Joiner.on("=?\n  AND ").join(filterValues.keySet()));
        query.append("=?\n");

        if (order!=null) {
            query.append("ORDER BY ").append(Joiner.on(", ").join(order.listFilters().keySet()));
        }
        query.append(";");
        return sql.query(query.toString(), filterValues.values().toArray(), new RowMapper<D>() {
            @Override
            public D mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final D row = NaiveDomain.newInstance(domainType);
                for (final Field field : fields.values()) {
                    ReflectionUtils.setField(field, row,rs.getObject(field.getName()));
                }
                return row;
            }
        });
    }
}
