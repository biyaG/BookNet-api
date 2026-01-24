package it.unipi.booknetapi.model.user;

import lombok.*;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@BsonDiscriminator(value = "admin")
public class Admin extends User {



}
