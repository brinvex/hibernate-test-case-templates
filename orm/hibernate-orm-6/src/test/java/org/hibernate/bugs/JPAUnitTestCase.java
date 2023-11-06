package org.hibernate.bugs;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
public class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
	}

	@After
	public void destroy() {
		entityManagerFactory.close();
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private Integer id;

		@Convert(converter = StringListConverter.class)
		private List<String> phoneNumbers;
	}

	public static class StringListConverter implements AttributeConverter<List<String>, String> {
		@Override
		public String convertToDatabaseColumn(List<String> elements) {
			return elements == null || elements.isEmpty() ? null : String.join(",", elements);
		}
		@Override
		public List<String> convertToEntityAttribute(String dbData) {
			return dbData == null ? null : List.of(dbData.split(","));
		}
	}

	// Entities are auto-discovered, so just add them anywhere on class-path
	// Add your tests, using standard JUnit.
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Test
	public void hhh123Test() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		Employee emp1 = new Employee();
		emp1.id = 1;
		emp1.phoneNumbers = List.of("0911 111 111", "0922 222 222");
		entityManager.persist(emp1);
		entityManager.flush();
		entityManager.clear();

		List<String> phoneNumbersJpql;
		{
			phoneNumbersJpql = (List<String>) entityManager
					.createQuery("select emp.phoneNumbers from Employee emp where emp.id = :EMP_ID")
					.setParameter("EMP_ID", emp1.id)
					.getSingleResult();
		}

		List<String> phoneNumbersCriteria;
		{
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<List> q = cb.createQuery(List.class);
			Root<Employee> r = q.from(Employee.class);
			q.select(r.get("phoneNumbers"));
			q.where(cb.equal(r.get("id"), emp1.id));
			phoneNumbersCriteria = (List<String>) entityManager.createQuery(q).getSingleResult();
		}

		assertEquals(emp1.phoneNumbers, phoneNumbersJpql);	//OK
		assertEquals(emp1.phoneNumbers, phoneNumbersCriteria); //OK in Hibernate 6.2, FAILS in Hibernate 6.3

		entityManager.getTransaction().commit();
		entityManager.close();
	}
}
