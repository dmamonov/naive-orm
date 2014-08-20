package org.naive.orm;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 2014-08-20 8:14 PM
 */
public abstract class NaiveDomain {
    private static final ConcurrentHashMap<Class<? extends NaiveDomain>, ImmutableMap<String, Field>> domainToFieldsMap = new ConcurrentHashMap<Class<? extends NaiveDomain>, ImmutableMap<String, Field>>();


    protected ImmutableMap<String, Field> getFields() {
        final ImmutableMap<String, Field> cachedFields = domainToFieldsMap.get(getClass());
        if (cachedFields != null) {
            return cachedFields;
        } else {
            final ImmutableMap.Builder<String, Field> builder = ImmutableMap.builder();
            ReflectionUtils.doWithFields(getClass(), new ReflectionUtils.FieldCallback() {
                @Override
                public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                    if (!Modifier.isStatic(field.getModifiers()) && field.isAccessible()) {
                        builder.put(field.getName(), field);
                    }
                }
            });
            final ImmutableMap<String, Field> collectedFields = builder.build();
            domainToFieldsMap.putIfAbsent(getClass(), collectedFields);
            return collectedFields;
        }
    }

    protected ImmutableMap<String, Object> listFilters() {
        return diffFrom(newInstance(getClass()));
    }

    public ImmutableMap<String, Object> diffFrom(final NaiveDomain other) {
        checkNotNull(other, "other required");
        checkArgument(this.getClass().isAssignableFrom(other.getClass()));
        final ImmutableMap.Builder<String, Object> diffBuilder = ImmutableMap.builder();
        for (final Field field : getFields().values()) {
            try {
                final Object thisValue;
                thisValue = field.get(this);
                if (!Objects.equal(thisValue, field.get(other))) {
                    diffBuilder.put(field.getName(), thisValue);
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Field " + field + " expected to be accessible");
            }
        }
        return diffBuilder.build();
    }

    protected static <D extends NaiveDomain> D newInstance(final Class<D> domainType) {
        checkNotNull(domainType, "domainType required");
        try {
            return domainType.newInstance();
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Blank constructor required for: " + domainType, e);
        }
    }
}
