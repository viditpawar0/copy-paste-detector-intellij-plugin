package org.lsrv.copypastedetector

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import java.awt.event.KeyEvent
import java.time.LocalDateTime
import javax.swing.KeyStroke

@Service
class KeyboardShortcutMonitor : AnActionListener {
    // Expanded list of restricted actions
    private val restrictedActions = listOf(
        // Settings-related actions
        "ShowSettings", "WelcomeScreen.Settings", "PluginSettings", "ConfigurePlugins",
        "Configure.Plugins.Selection", "ShowProjectStructureSettings", "ShowModulePropertiesSettings",
        "ShowApplicationSettings", "ShowSettingsAndFindUsages",
        
        // File operations that could be used to bypass restrictions
        "ManageRecentProjects", "NewScratchFile", "Terminal.OpenInTerminal", 
        "ExternalSystem.OpenProjectDir", "ShowFilePath", "RevealIn", "OpenFile", 
        "OpenDirectoryProject", "OpenFile", "SaveAll", "ExportToHTML", "Print",
        
        // System operations
        "ExportSettings", "ImportSettings", "CheckForUpdate", "PluginUpdate",
        "EditCustomProperties", "EditCustomVmOptions", "RestartIde", "Exit",
        
        // External tools
        "ExternalToolsGroup", "ExternalTools", "Tool_External Tools",
        
        // Plugin management
        "Plugins.DisablePlugins", "Plugins.EnablePlugins", "Plugins.InstallPlugin", 
        "Plugins.UninstallPlugin",
        
        // Browser actions
        "OpenInBrowser", "WebOpenInAction", "OpenInBrowserGroup",
        
        // Version control that could be used to restore previous versions
        "Vcs.ShowTabbedFileHistory", "Git.Log", "Git.Menu", "Vcs.ShowHistoryForBlock",
        
        // Run/Debug configuration
        "editRunConfigurations", "RunConfiguration", "RunMenu", "ChooseRunConfiguration",
        
        // Other potentially dangerous actions
        "TogglePresentationMode", "ToggleFullScreen", "ToggleDistractionFreeMode",
        "HideAllWindows", "QuickChangeScheme", "RecentChanges", "GotoAction",
        "SearchEverywhere", "GotoClass", "GotoFile", "GotoSymbol"
    )
    
    // List of restricted keyboard shortcuts (key codes)
    private val restrictedKeyCombinations = listOf(
        // Common settings shortcuts
        KeyCombination(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK), // Ctrl+, (Windows/Linux)
        KeyCombination(KeyEvent.VK_COMMA, KeyEvent.META_DOWN_MASK), // Cmd+, (Mac)
        
        // Alt+Enter (intentions menu)
        KeyCombination(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK),
        
        // Ctrl+Alt+S (Settings)
        KeyCombination(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK or KeyEvent.ALT_DOWN_MASK),
        
        // Ctrl+Shift+A (Find Action)
        KeyCombination(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK),
        KeyCombination(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK),
        
        // Double Shift (Search Everywhere)
        KeyCombination(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK),
        
        // Ctrl+Alt+F (Add to Favorites)
        KeyCombination(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK or KeyEvent.ALT_DOWN_MASK),
        
        // Alt+` (VCS Operations Popup)
        KeyCombination(KeyEvent.VK_BACK_QUOTE, KeyEvent.ALT_DOWN_MASK),
        
        // Ctrl+Shift+F12 (Hide All Windows)
        KeyCombination(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK or KeyEvent.SHIFT_DOWN_MASK)
    )
    
    // Track key presses for double-shift detection
    private var lastShiftPressTime = 0L
    
    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        val actionId = ActionManager.getInstance().getId(action)
        if (actionId != null && restrictedActions.contains(actionId)) {
            val sessionData = SessionData.getInstance().state
            if (!sessionData.sessionId.isNullOrEmpty()) {
                // Log the restricted action
                println("WARNING: Restricted action attempted: $actionId")
                println("  Session ID: ${sessionData.sessionId}")
                println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
                println("  Timestamp: ${LocalDateTime.now()}")
                
                // Notify the user
                val project = ProjectManager.getInstance().openProjects.firstOrNull()
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showWarningDialog(
                            project,
                            "The action you attempted ($actionId) is restricted during the exam.\n\n" +
                            "Access to settings and system configuration is not allowed during the exam.\n\n" +
                            "This activity has been logged.",
                            "Restricted Action"
                        )
                    }
                }
                
                // Consume the event to prevent the action
                event.presentation.isEnabled = false
            }
        }
    }
    
    // Block keyboard shortcuts
    override fun beforeEditorTyping(c: Char, dataContext: DataContext) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // This is a simplified approach - we're just checking for comma (common settings shortcut)
        if (c == ',') {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showWarningDialog(
                        project,
                        "Access to settings is restricted during the exam.\n\n" +
                        "This activity has been logged.",
                        "Restricted Action"
                    )
                }
            }
        }
    }
    
    // Add a method to intercept key events globally
    // This would need to be registered with a global key listener
    fun processKeyEvent(e: KeyEvent) {
        val sessionData = SessionData.getInstance().state
        if (sessionData.sessionId.isNullOrEmpty()) {
            return
        }
        
        // Check for double-shift (Search Everywhere)
        if (e.keyCode == KeyEvent.VK_SHIFT && e.id == KeyEvent.KEY_PRESSED) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastShiftPressTime < 300) { // 300ms threshold for double-press
                blockKeyboardShortcut("Double Shift (Search Everywhere)")
            }
            lastShiftPressTime = currentTime
            return
        }
        
        // Check other restricted key combinations
        for (combo in restrictedKeyCombinations) {
            if (e.keyCode == combo.keyCode && (e.modifiersEx and combo.modifiers) == combo.modifiers) {
                blockKeyboardShortcut("Keyboard shortcut: ${getKeyText(e)}")
                e.consume() // Try to consume the event
                break
            }
        }
    }
    
    private fun blockKeyboardShortcut(shortcutDescription: String) {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showWarningDialog(
                    project,
                    "The keyboard shortcut you attempted ($shortcutDescription) is restricted during the exam.\n\n" +
                    "This activity has been logged.",
                    "Restricted Action"
                )
            }
        }
        
        val sessionData = SessionData.getInstance().state
        println("WARNING: Restricted keyboard shortcut attempted: $shortcutDescription")
        println("  Session ID: ${sessionData.sessionId}")
        println("  User: ${sessionData.clientName} (${sessionData.rollNumber})")
        println("  Timestamp: ${LocalDateTime.now()}")
    }
    
    private fun getKeyText(e: KeyEvent): String {
        val modifiers = KeyEvent.getModifiersExText(e.modifiersEx)
        val key = KeyEvent.getKeyText(e.keyCode)
        return if (modifiers.isEmpty()) key else "$modifiers+$key"
    }
    
    // Helper class to represent key combinations
    private data class KeyCombination(val keyCode: Int, val modifiers: Int)
}