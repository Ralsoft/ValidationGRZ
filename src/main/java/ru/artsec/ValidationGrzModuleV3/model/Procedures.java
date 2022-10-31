package ru.artsec.ValidationGrzModuleV3.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@NamedStoredProcedureQuery(
        name = "validatepass",
        procedureName = "validatepass", parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "ID_DEV", type = Integer.class),
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "ID_CARD", type = String.class),
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "GRZ", type = String.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "EVENT_TYPE", type = String.class),
        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "ID_PEP", type = String.class),
})
public class Procedures {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Procedures procedures = (Procedures) o;
        return id != null && Objects.equals(id, procedures.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
