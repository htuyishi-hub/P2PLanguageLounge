package tech.isano.language.lounge

sealed interface LoungeUiState {
    data object Idle : LoungeUiState
    data object PermissionPrimer : LoungeUiState
    data object Scanning : LoungeUiState
    data object RoomEmpty_Listening : LoungeUiState
    data class Connected(val peerId: String) : LoungeUiState
}
