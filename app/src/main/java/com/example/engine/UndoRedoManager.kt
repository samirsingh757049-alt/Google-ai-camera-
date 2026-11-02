package com.example.engine

import com.example.ui.viewmodel.EditorSnapshot
import java.util.UUID

data class HistoryStep(
    val id: String = UUID.randomUUID().toString(),
    val actionName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val snapshot: EditorSnapshot
)

class UndoRedoManager(private val maxCapacity: Int = 40) {

    private val undoStack = mutableListOf<HistoryStep>()
    private val redoStack = mutableListOf<HistoryStep>()

    var lastActionCategory: String? = null
        private set

    val undoCount: Int
        get() = undoStack.size

    val redoCount: Int
        get() = redoStack.size

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun getUndoHistory(): List<HistoryStep> = undoStack.toList()

    fun getRedoHistory(): List<HistoryStep> = redoStack.toList()

    /**
     * Records a new edit step into the undo stack and clears redo stack.
     */
    fun recordStep(actionName: String, snapshot: EditorSnapshot, category: String? = null) {
        // Coalesce continuous rapid edits for the same slider category
        if (category != null && category == lastActionCategory && undoStack.isNotEmpty()) {
            // Update the latest step instead of bloating stack
            val last = undoStack.removeAt(undoStack.lastIndex)
            undoStack.add(last.copy(actionName = actionName, snapshot = snapshot, timestamp = System.currentTimeMillis()))
        } else {
            undoStack.add(
                HistoryStep(
                    actionName = actionName,
                    snapshot = snapshot
                )
            )
            if (undoStack.size > maxCapacity) {
                undoStack.removeAt(0)
            }
        }
        lastActionCategory = category
        redoStack.clear()
    }

    /**
     * Finalizes continuous editing sequence (e.g. slider release).
     */
    fun finishContinuousEdit() {
        lastActionCategory = null
    }

    /**
     * Undoes the last action, moving current state to redo stack.
     * Returns the previous snapshot and the undone action name.
     */
    fun undo(currentSnapshot: EditorSnapshot): Pair<String, EditorSnapshot>? {
        if (undoStack.isEmpty()) return null
        lastActionCategory = null

        val previous = undoStack.removeAt(undoStack.lastIndex)

        // Push current state onto redo stack
        redoStack.add(
            HistoryStep(
                actionName = previous.actionName,
                snapshot = currentSnapshot
            )
        )
        if (redoStack.size > maxCapacity) {
            redoStack.removeAt(0)
        }

        return Pair(previous.actionName, previous.snapshot)
    }

    /**
     * Redoes the last undone action.
     * Returns the next snapshot and the redone action name.
     */
    fun redo(currentSnapshot: EditorSnapshot): Pair<String, EditorSnapshot>? {
        if (redoStack.isEmpty()) return null
        lastActionCategory = null

        val next = redoStack.removeAt(redoStack.lastIndex)

        // Push current state back to undo stack
        undoStack.add(
            HistoryStep(
                actionName = next.actionName,
                snapshot = currentSnapshot
            )
        )

        return Pair(next.actionName, next.snapshot)
    }

    /**
     * Jumps directly to a specific historical step in the undo stack.
     */
    fun jumpToUndoStep(stepId: String, currentSnapshot: EditorSnapshot): EditorSnapshot? {
        val targetIndex = undoStack.indexOfFirst { it.id == stepId }
        if (targetIndex < 0) return null

        lastActionCategory = null
        val targetStep = undoStack[targetIndex]

        // Push current state into redo stack
        redoStack.add(
            HistoryStep(
                actionName = "Revert to: ${targetStep.actionName}",
                snapshot = currentSnapshot
            )
        )

        // Move items after target into redo stack in reverse
        while (undoStack.size > targetIndex + 1) {
            val popped = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(popped)
        }

        undoStack.removeAt(targetIndex)
        return targetStep.snapshot
    }

    /**
     * Clears all history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastActionCategory = null
    }
}
