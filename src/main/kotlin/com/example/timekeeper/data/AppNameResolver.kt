package com.example.timekeeper.data

import com.example.timekeeper.domain.AppCategory
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.filechooser.FileSystemView

/**
 * Roblox installs each build into its own `Versions\version-<hash>\` folder, so
 * the exe path changes on every update. The tracker credits programs by path, so
 * without this a Roblox update would fork its history into a new row. Folding the
 * hash away keeps every build under one path. The pattern is Roblox-specific, so
 * nothing else is affected.
 */
private val ROBLOX_VERSION_SEGMENT = Regex("""[\\/]version-[0-9a-f]+[\\/]""", RegexOption.IGNORE_CASE)

// Two backslashes in the replacement: a lone `\` is itself an escape character in
// a regex replacement, so it takes `\\` to mean one literal path separator.
internal fun normalizePath(exePath: String): String =
    exePath.replace(ROBLOX_VERSION_SEGMENT, "\\\\")

/**
 * Turns an executable path into something a human wants to look at: a friendly
 * name, the program's real icon, and a category.
 *
 * A Windows exe carries no app-store metadata, so naming and categorising lean
 * on a curated map keyed by the exe's file name (`chrome.exe` → "Google Chrome"),
 * with a path heuristic for games and a tidy-up fallback for everything else.
 */
class AppNameResolver {

    private val labelCache = HashMap<String, String>()
    private val categoryCache = HashMap<String, AppCategory>()
    private val iconCache = HashMap<String, Image?>()

    /** The program's file name without extension, lowercased ("chrome"). */
    private fun baseName(exePath: String): String =
        File(exePath).name.removeSuffix(".exe").removeSuffix(".EXE").lowercase()

    fun label(exePath: String): String = labelCache.getOrPut(exePath) {
        val base = baseName(normalizePath(exePath))
        knownLabels[base] ?: prettify(base)
    }

    fun category(exePath: String): AppCategory = categoryCache.getOrPut(exePath) {
        val path = normalizePath(exePath)
        knownCategories[baseName(path)] ?: pathCategory(path) ?: AppCategory.OTHER
    }

    /**
     * The exe's icon, as Windows draws it. Swing's [FileSystemView] asks the
     * shell for the file's associated icon, which for a program is its own icon;
     * we paint that [javax.swing.Icon] onto a bitmap so it can cross into Compose.
     */
    fun icon(exePath: String): Image? = iconCache.getOrPut(exePath) {
        try {
            val file = iconFileFor(exePath) ?: return@getOrPut null
            val icon = FileSystemView.getFileSystemView().getSystemIcon(file)
            val w = icon.iconWidth.coerceAtLeast(1)
            val h = icon.iconHeight.coerceAtLeast(1)
            BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).also { img ->
                val g = img.createGraphics()
                icon.paintIcon(null, g, 0, 0)
                g.dispose()
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * The file to ask Windows for an icon. Usually the exe itself, but Roblox is
     * special: the path we record has its version folder folded away, and the old
     * build may be gone after an update — so fall back to whatever build is on
     * disk now.
     */
    private fun iconFileFor(exePath: String): File? {
        File(exePath).takeIf { it.isFile }?.let { return it }
        val name = File(exePath).name
        // Roblox's collapsed path points one folder above the live build, e.g.
        // Versions\RobloxPlayerBeta.exe vs Versions\version-<hash>\RobloxPlayerBeta.exe.
        // Search the parent directory and one level of its subdirectories.
        val dir = File(normalizePath(exePath)).parentFile ?: return null
        fun findIn(d: File) = d.listFiles { f -> f.isFile && f.name.equals(name, ignoreCase = true) }?.firstOrNull()
        findIn(dir)?.let { return it }
        return dir.listFiles { f -> f.isDirectory }
            ?.asSequence()
            ?.mapNotNull { findIn(it) }
            ?.firstOrNull()
    }

    /** "some_tool-64" → "Some Tool 64". Keeps unknown programs readable. */
    private fun prettify(base: String): String =
        base.replace('_', ' ').replace('-', ' ').replace('.', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .ifBlank { base }

    /** Games installed by a launcher into a well-known folder, caught by path. */
    private fun pathCategory(exePath: String): AppCategory? {
        val p = exePath.lowercase()
        return if (gameMarkers.any { p.contains(it) }) AppCategory.GAMES else null
    }

    private companion object {
        val gameMarkers = listOf(
            "\\steamapps\\",
            "\\epic games\\",
            "\\riot games\\",
            "\\battle.net\\",
            "\\gog galaxy\\",
            "\\ubisoft\\",
            "\\ea games\\",
            "\\origin games\\",
            "\\roblox\\versions\\",
        )

        /** Friendly names for the programs most people actually run. */
        val knownLabels: Map<String, String> = mapOf(
            "chrome" to "Google Chrome",
            "msedge" to "Microsoft Edge",
            "firefox" to "Firefox",
            "brave" to "Brave",
            "opera" to "Opera",
            "vivaldi" to "Vivaldi",
            "zen" to "Zen Browser",
            "code" to "Visual Studio Code",
            "code - insiders" to "VS Code Insiders",
            "devenv" to "Visual Studio",
            "idea64" to "IntelliJ IDEA",
            "pycharm64" to "PyCharm",
            "webstorm64" to "WebStorm",
            "androidstudio64" to "Android Studio",
            "sublime_text" to "Sublime Text",
            "notepad++" to "Notepad++",
            "notepad" to "Notepad",
            "explorer" to "File Explorer",
            "winword" to "Microsoft Word",
            "excel" to "Microsoft Excel",
            "powerpnt" to "Microsoft PowerPoint",
            "outlook" to "Microsoft Outlook",
            "onenote" to "Microsoft OneNote",
            "discord" to "Discord",
            "telegram" to "Telegram",
            "whatsapp" to "WhatsApp",
            "slack" to "Slack",
            "teams" to "Microsoft Teams",
            "ms-teams" to "Microsoft Teams",
            "zoom" to "Zoom",
            "spotify" to "Spotify",
            "vlc" to "VLC",
            "mpv" to "mpv",
            "obs64" to "OBS Studio",
            "obsidian" to "Obsidian",
            "notion" to "Notion",
            "steam" to "Steam",
            "steamwebhelper" to "Steam",
            "robloxplayerbeta" to "Roblox",
            "robloxstudiobeta" to "Roblox Studio",
            "epicgameslauncher" to "Epic Games",
            "galaxyclient" to "GOG Galaxy",
            "blender" to "Blender",
            "photoshop" to "Adobe Photoshop",
            "illustrator" to "Adobe Illustrator",
            "acrobat" to "Adobe Acrobat",
            "winrar" to "WinRAR",
            "cmd" to "Command Prompt",
            "powershell" to "PowerShell",
            "windowsterminal" to "Windows Terminal",
        )

        val knownCategories: Map<String, AppCategory> = mapOf(
            // Social
            "discord" to AppCategory.SOCIAL,
            "telegram" to AppCategory.SOCIAL,
            "whatsapp" to AppCategory.SOCIAL,
            "skype" to AppCategory.SOCIAL,

            // Video / audio
            "vlc" to AppCategory.VIDEO,
            "mpv" to AppCategory.VIDEO,
            "spotify" to AppCategory.VIDEO,
            "netflix" to AppCategory.VIDEO,
            "obs64" to AppCategory.VIDEO,
            "potplayermini64" to AppCategory.VIDEO,

            // Games (launchers)
            "steam" to AppCategory.GAMES,
            "steamwebhelper" to AppCategory.GAMES,
            "robloxplayerbeta" to AppCategory.GAMES,
            "robloxstudiobeta" to AppCategory.GAMES,
            "epicgameslauncher" to AppCategory.GAMES,
            "galaxyclient" to AppCategory.GAMES,
            "battle.net" to AppCategory.GAMES,
            "riotclientservices" to AppCategory.GAMES,

            // Productivity
            "chrome" to AppCategory.PRODUCTIVITY,
            "msedge" to AppCategory.PRODUCTIVITY,
            "firefox" to AppCategory.PRODUCTIVITY,
            "brave" to AppCategory.PRODUCTIVITY,
            "opera" to AppCategory.PRODUCTIVITY,
            "vivaldi" to AppCategory.PRODUCTIVITY,
            "zen" to AppCategory.PRODUCTIVITY,
            "code" to AppCategory.PRODUCTIVITY,
            "devenv" to AppCategory.PRODUCTIVITY,
            "idea64" to AppCategory.PRODUCTIVITY,
            "pycharm64" to AppCategory.PRODUCTIVITY,
            "webstorm64" to AppCategory.PRODUCTIVITY,
            "androidstudio64" to AppCategory.PRODUCTIVITY,
            "sublime_text" to AppCategory.PRODUCTIVITY,
            "notepad++" to AppCategory.PRODUCTIVITY,
            "notepad" to AppCategory.PRODUCTIVITY,
            "winword" to AppCategory.PRODUCTIVITY,
            "excel" to AppCategory.PRODUCTIVITY,
            "powerpnt" to AppCategory.PRODUCTIVITY,
            "outlook" to AppCategory.PRODUCTIVITY,
            "onenote" to AppCategory.PRODUCTIVITY,
            "teams" to AppCategory.PRODUCTIVITY,
            "ms-teams" to AppCategory.PRODUCTIVITY,
            "slack" to AppCategory.PRODUCTIVITY,
            "zoom" to AppCategory.PRODUCTIVITY,
            "obsidian" to AppCategory.PRODUCTIVITY,
            "notion" to AppCategory.PRODUCTIVITY,
            "windowsterminal" to AppCategory.PRODUCTIVITY,
            "cmd" to AppCategory.PRODUCTIVITY,
            "powershell" to AppCategory.PRODUCTIVITY,
            "blender" to AppCategory.PRODUCTIVITY,
            "photoshop" to AppCategory.PRODUCTIVITY,
            "acrobat" to AppCategory.PRODUCTIVITY,
        )

        /**
         * Programs that shouldn't appear in the ranked list: the Windows shell
         * (desktop, taskbar, File Explorer) and the lock screen. Time with the
         * desktop in front isn't "an app you chose to spend time in".
         */
        val ignoredExeNames: Set<String> = setOf("explorer", "lockapp", "shellexperiencehost")
    }

    fun isIgnored(exePath: String): Boolean = baseName(normalizePath(exePath)) in ignoredExeNames
}
