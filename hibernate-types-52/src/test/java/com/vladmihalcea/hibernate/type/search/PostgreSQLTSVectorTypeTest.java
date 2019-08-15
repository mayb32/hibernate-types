package com.vladmihalcea.hibernate.type.search;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.vladmihalcea.hibernate.type.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 * @author Philip Riecks
 */
public class PostgreSQLTSVectorTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Book.class
        };
    }

    @Override
    public void afterInit() {
        /*doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO book (id, isbn, text) VALUES (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', to_tsvector('This book" +
                        " is a journey into Java data access performance tuning. From connection management, to batch" +
                        " updates, fetch sizes and concurrency control mechanisms, it unravels the inner workings of" +
                        " the most common Java data access frameworks'))");
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });*/
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setId(1L);
            book.setIsbn("978-9730228236");
            book.setFts(
                    "This book is a journey into Java data access performance tuning. From connection management, to batch" +
                    " updates, fetch sizes and concurrency control mechanisms, it unravels the inner workings of" +
                    " the most common Java data access frameworks."
            );
            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1L);

/*            assertTrue(book.getFts().contains(":"));
            assertTrue(book.getFts().contains("'"));
            assertTrue(book.getFts().contains("java"));
            assertTrue(book.getFts().contains("size"));
            assertTrue(book.getFts().contains("access"));
            assertTrue(book.getFts().contains("batch"));
            assertTrue(book.getFts().contains("book"));*/

            Tuple tuple = (Tuple) entityManager.createNativeQuery(
                "SELECT " +
                "   fts @@ to_tsquery('Java') as contain_java " +
                "FROM book", Tuple.class)
            .getSingleResult();

            assertEquals(true, tuple.get("contain_java"));
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @TypeDef(name = "tsvector", typeClass = PostgreSQLTSVectorType.class)
    public static class Book {

        @Id
        private Long id;

        @NaturalId
        private String isbn;

        @Type(type = "tsvector")
        @Column(columnDefinition = "tsvector")
        private String fts;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getFts() {
            return fts;
        }

        public void setFts(String fts) {
            this.fts = fts;
        }
    }
}
