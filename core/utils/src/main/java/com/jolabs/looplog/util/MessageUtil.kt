sealed class UiMessage {
    data class StringRes(val resId: Int) : UiMessage()
    data class Text(val message: String) : UiMessage()
}