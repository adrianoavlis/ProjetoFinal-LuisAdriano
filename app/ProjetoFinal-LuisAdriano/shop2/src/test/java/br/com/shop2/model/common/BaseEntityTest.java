package br.com.shop2.model.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    private static class DummyEntity extends BaseEntity {
    }

    private static class AnotherDummyEntity extends BaseEntity {
    }

    @Test
    void newEntitiesAreNotEqual() {
        DummyEntity first = new DummyEntity();
        DummyEntity second = new DummyEntity();

        assertNotEquals(first, second, "Entidades novas não devem ser iguais");
        assertNotEquals(first.hashCode(), second.hashCode(), "Entidades novas devem ter hash codes distintos");
    }

    @Test
    void newEntityIsNotEqualToPersistedEntity() {
        DummyEntity fresh = new DummyEntity();
        DummyEntity persisted = new DummyEntity();
        persisted.setId(1L);

        assertNotEquals(fresh, persisted, "Entidade sem ID não deve ser igual a entidade persistida");
        assertNotEquals(persisted, fresh, "Comparação deve ser simétrica");
    }

    @Test
    void persistedEntitiesWithSameIdAreEqual() {
        DummyEntity first = new DummyEntity();
        first.setId(10L);
        DummyEntity second = new DummyEntity();
        second.setId(10L);

        assertEquals(first, second, "Entidades persistidas com o mesmo ID devem ser iguais");
        assertEquals(first.hashCode(), second.hashCode(), "Hashes devem ser iguais para entidades persistidas com o mesmo ID");
    }

    @Test
    void persistedEntitiesWithSameIdButDifferentTypesAreNotEqual() {
        DummyEntity dummy = new DummyEntity();
        dummy.setId(5L);
        AnotherDummyEntity anotherDummy = new AnotherDummyEntity();
        anotherDummy.setId(5L);

        assertNotEquals(dummy, anotherDummy, "Entidades de classes diferentes não devem ser iguais");
    }
}
