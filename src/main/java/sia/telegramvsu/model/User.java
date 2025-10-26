package sia.telegramvsu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "users")
@Data
public class User {
    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "group_name")
    private String group;

    private String status;

    @Column(columnDefinition = "BOOLEAN DEFAULT false", name = "newsletter")
    private boolean newsletter;
}
