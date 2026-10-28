/**
 * A generic class that represents the state of a resource.
 * This can be used to handle data fetched from a repository,
 * providing states for loading, success, and error.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {

    /**
     * Represents a successful state with a data payload.
     * @param data The data received from the successful operation.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents a loading state. This is typically used to show a progress indicator.
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    /**
     * Represents an error state with an optional error message.
     * @param message The error message to display.
     * @param data Optional data that might be available even in an error state.
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}
