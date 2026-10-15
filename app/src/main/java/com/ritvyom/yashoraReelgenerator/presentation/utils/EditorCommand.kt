package com.ritvyom.yashoraReelgenerator.presentation.utils

import com.ritvyom.yashoraReelgenerator.domain.models.Scene
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

/**
 * Command interface defining an executable, reversible action in the Video Editor.
 */
interface EditorCommand {
    val actionName: String
    fun execute()
    fun undo()
}

/**
 * State-based snapshot command recording before and after states of video timeline clips and settings.
 */
class SceneStateCommand(
    override val actionName: String,
    private val previousState: List<Scene>,
    private val nextState: List<Scene>,
    private val applyState: (List<Scene>) -> Unit
) : EditorCommand {

    override fun execute() {
        applyState(nextState.map { it.copy() })
    }

    override fun undo() {
        applyState(previousState.map { it.copy() })
    }
}

/**
 * Snapshot entry in the timeline undo/redo stack.
 */
data class TimelineHistoryEntry(
    val scenes: List<Scene>,
    val actionName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-performance state-based Command Pattern Manager for the Multi-Scene Timeline Editor.
 * Maintains undo/redo stacks, memory limits, atomic transactions for continuous gestures,
 * and reactive StateFlows so users can seamlessly revert and redo timeline changes.
 */
class EditorCommandManager(
    private val maxHistorySize: Int = 60
) {
    private val undoStack = Stack<TimelineHistoryEntry>()
    private val redoStack = Stack<TimelineHistoryEntry>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _lastActionName = MutableStateFlow<String?>("Ready")
    val lastActionName: StateFlow<String?> = _lastActionName.asStateFlow()

    private val _undoCount = MutableStateFlow(0)
    val undoCount: StateFlow<Int> = _undoCount.asStateFlow()

    private val _redoCount = MutableStateFlow(0)
    val redoCount: StateFlow<Int> = _redoCount.asStateFlow()

    // Transaction state for continuous gestures (scrub, trim drag, slider drag)
    private var transactionInitialState: List<Scene>? = null
    private var transactionActionName: String? = null

    /**
     * Records a new state onto the undo stack.
     * Prevents duplicate identical states and clears redo stack.
     */
    fun recordState(scenes: List<Scene>, actionName: String = "Edit Timeline") {
        val snapshot = scenes.map { it.copy() }

        // Ignore duplicate consecutive states
        if (undoStack.isNotEmpty() && areScenesEqual(undoStack.peek().scenes, snapshot)) {
            return
        }

        undoStack.push(TimelineHistoryEntry(snapshot, actionName))
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _lastActionName.value = actionName
        updateState()
    }

    /**
     * Begins an atomic transaction for continuous gestures (e.g. dragging trim handles).
     * Prevents gesture spam from filling the undo history with intermediate values.
     */
    fun beginTransaction(actionName: String, scenes: List<Scene>) {
        if (transactionInitialState == null) {
            transactionInitialState = scenes.map { it.copy() }
            transactionActionName = actionName
        }
    }

    /**
     * Commits the active transaction when the gesture ends.
     * Pushes exactly ONE undo step representing the full gesture change.
     */
    fun commitTransaction(finalScenes: List<Scene>) {
        val initial = transactionInitialState ?: return
        val action = transactionActionName ?: "Edit Timeline"
        transactionInitialState = null
        transactionActionName = null

        val finalSnapshot = finalScenes.map { it.copy() }
        if (!areScenesEqual(initial, finalSnapshot)) {
            // Push the initial state as the restore point
            if (undoStack.isEmpty() || !areScenesEqual(undoStack.peek().scenes, initial)) {
                undoStack.push(TimelineHistoryEntry(initial, action))
                if (undoStack.size > maxHistorySize) {
                    undoStack.removeAt(0)
                }
            }
            redoStack.clear()
            _lastActionName.value = action
            updateState()
        }
    }

    /**
     * Cancels an in-progress transaction without recording to history.
     */
    fun cancelTransaction() {
        transactionInitialState = null
        transactionActionName = null
    }

    /**
     * Undoes the last change, popping from undoStack and pushing to redoStack.
     * Invokes [onApply] with the restored scenes and the action name.
     */
    fun undo(currentScenes: List<Scene>, onApply: (restored: List<Scene>, actionName: String) -> Unit): Boolean {
        if (undoStack.isNotEmpty()) {
            val currentSnapshot = currentScenes.map { it.copy() }
            val entryToRestore = undoStack.pop()

            val actionName = entryToRestore.actionName
            redoStack.push(TimelineHistoryEntry(currentSnapshot, actionName))
            if (redoStack.size > maxHistorySize) {
                redoStack.removeAt(0)
            }

            _lastActionName.value = "Undid: $actionName"
            updateState()

            onApply(entryToRestore.scenes.map { it.copy() }, actionName)
            return true
        }
        return false
    }

    /**
     * Redoes the last undone change, popping from redoStack and pushing to undoStack.
     * Invokes [onApply] with the reapplied scenes and the action name.
     */
    fun redo(currentScenes: List<Scene>, onApply: (restored: List<Scene>, actionName: String) -> Unit): Boolean {
        if (redoStack.isNotEmpty()) {
            val currentSnapshot = currentScenes.map { it.copy() }
            val entryToReapply = redoStack.pop()

            val actionName = entryToReapply.actionName
            undoStack.push(TimelineHistoryEntry(currentSnapshot, actionName))
            if (undoStack.size > maxHistorySize) {
                undoStack.removeAt(0)
            }

            _lastActionName.value = "Redid: $actionName"
            updateState()

            onApply(entryToReapply.scenes.map { it.copy() }, actionName)
            return true
        }
        return false
    }

    /**
     * Clears all history stacks.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        transactionInitialState = null
        transactionActionName = null
        _lastActionName.value = "Ready"
        updateState()
    }

    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _undoCount.value = undoStack.size
        _redoCount.value = redoStack.size
    }

    private fun areScenesEqual(listA: List<Scene>, listB: List<Scene>): Boolean {
        return listA == listB
    }
}
