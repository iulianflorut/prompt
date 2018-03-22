package prompt;

import java.util.Optional;
import java.util.function.Consumer;

public class PropertyConsumer<T> {

	public final  T property;
	public final Consumer<T> consumer;

	public PropertyConsumer(T property, Consumer<T> consumer) {
		this.property = property;
		this.consumer = consumer;
	}
	
	public static <T> Optional<PropertyConsumer<T>> optional(T property, Consumer<T> consumer) {
		return Optional.of(new PropertyConsumer<T>(property, consumer));
	}

}
