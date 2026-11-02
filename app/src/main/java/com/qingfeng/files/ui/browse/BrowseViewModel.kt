package com.qingfeng.files.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.qingfeng.files.data.Category
import com.qingfeng.files.data.Clipboard
import com.qingfeng.files.data.FileError
import com.qingfeng.files.data.FileItem
import com.qingfeng.files.data.FilePrefs
import com.qingfeng.files.data.FileSource
import com.qingfeng.files.data.Filter
import com.qingfeng.files.data.ProgressCallback
import com.qingfeng.files.data.RecycleBin
import com.qingfeng.files.data.FileSources
import com.qingfeng.files.data.SearchScope
import com.qingfeng.files.data.SortState
import com.qingfeng.files.data.ViewMode
import com.qingfeng.files.data.sortedBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 浏览页 ViewModel。
 *
 * 三条数据流互斥：搜索 > 普通浏览（含筛选/排序）。
 */
class BrowseViewModel(app: Application) : AndroidViewModel(app) {

    private val source: FileSource = FileSources.local()
    private val recycleBin = RecycleBin.from(getApplication())
    private val prefs = FilePrefs(app)
    private val pathStack = ArrayDeque<String>()

    /** 未经筛选排序的原始列表 */
    private var rawItems: List<FileItem> = emptyList()

    private val _items = MutableLiveData<List<FileItem>>()
    val items: LiveData<List<FileItem>> = _items

    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _canGoUp = MutableLiveData<Boolean>()
    val canGoUp: LiveData<Boolean> = _canGoUp

    private var filter: Filter = Filter()
    private var sortState: SortState = SortState()

    // ---------- 视图模式 ----------
    private val _viewMode = MutableLiveData<ViewMode>()
    val viewMode: LiveData<ViewMode> = _viewMode

    // ---------- 剪贴板（两步式复制/移动） ----------
    private val _clipboard = MutableLiveData<Clipboard?>()
    val clipboard: LiveData<Clipboard?> = _clipboard

    // ---------- 搜索 ----------
    private val _searchResults = MutableLiveData<List<FileItem>?>()
    val searchResults: LiveData<List<FileItem>?> = _searchResults

    // ---------- 长任务进度（可取消） ----------
    private val _progress = MutableLiveData<Pair<Long, Long>?>()
    val progress: LiveData<Pair<Long, Long>?> = _progress
    private val _cancel = MutableLiveData<Boolean>()
    val cancel: LiveData<Boolean> = _cancel

    private val _hiddenOn = MutableLiveData<Boolean>(false)
    val hiddenOn: LiveData<Boolean> = _hiddenOn

    private val _trashCount = MutableLiveData<Int>(0)
    val trashCount: LiveData<Int> = _trashCount

    private val _searchScope = MutableLiveData<SearchScope>()
    val searchScope: LiveData<SearchScope> = _searchScope

    init {
        _searchScope.value = SearchScope.ALL
        filter = filter.copy(showHidden = prefs.showHidden)
        _hiddenOn.value = prefs.showHidden
    }

    fun start() {
        if (pathStack.isEmpty()) {
            val root = source.rootPath()
            pathStack.addLast(root)
            _viewMode.value = prefs.viewModeFor(root)
            refreshTrashCount()
            open(root)
        }
    }

    fun open(path: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _currentPath.value = path
            _canGoUp.value = pathStack.size > 1
            _viewMode.value = prefs.viewModeFor(path)
            try {
                val list = withContext(Dispatchers.IO) { source.list(path) }
                rawItems = list
                publish()
            } catch (e: FileError) {
                _error.value = e.message
                rawItems = emptyList(); publish()
            } catch (e: Exception) {
                _error.value = e.message ?: "读取失败"
                rawItems = emptyList(); publish()
            } finally {
                _loading.value = false
            }
        }
    }

    /** 筛选 + 排序后推给 UI */
    private fun publish() {
        _items.value = rawItems.filter { filter.match(it) }.sortedBy(sortState)
    }

    fun applyFilter(f: Filter) {
        filter = f
        prefs.showHidden = f.showHidden
        publish()
    }

    fun applySort(s: SortState) {
        sortState = s
        publish()
    }

    fun currentFilter(): Filter = filter
    fun currentSort(): SortState = sortState

    fun setViewMode(mode: ViewMode, applyAll: Boolean) {
        prefs.setViewMode(currentPathValue(), mode, applyAll)
        _viewMode.value = mode
    }

    fun enterDirectory(item: FileItem) {
        if (!item.isDirectory) return
        pathStack.addLast(item.path)
        open(item.path)
    }

    fun goUp(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeLast()
        open(pathStack.last())
        return true
    }

    // ---------- 剪贴板操作（两步式） ----------
    fun putClipboard(items: List<FileItem>, mode: Clipboard.Mode) {
        if (items.isEmpty()) return
        _clipboard.value = Clipboard(items.toList(), mode)
    }

    fun clearClipboard() { _clipboard.value = null }

    fun pasteHere() {
        val cb = _clipboard.value ?: return
        viewModelScope.launch {
            _progress.value = 0 to 1
            try {
                val dir = currentPathValue()
                val callback = object : ProgressCallback {
                    override fun onProgress(done: Long, total: Long) {
                        _progress.postValue(done to total.coerceAtLeast(1))
                    }
                    override fun isCancelled() = _cancel.value == true
                }
                withContext(Dispatchers.IO) {
                    if (cb.mode == Clipboard.Mode.COPY) source.copyAll(cb.items, dir, callback)
                    else source.moveAll(cb.items, dir, callback)
                }
                _clipboard.value = null
                _progress.value = null
                open(dir)
            } catch (e: FileError) { _error.value = e.message; _progress.value = null }
            catch (e: Exception) { _error.value = e.message ?: "粘贴失败"; _progress.value = null }
        }
    }

    fun cancelPaste() { _cancel.value = true }

    // ---------- 显示隐藏文件（提到主面板，参照物面板自带 CheckBox） ----------
    fun toggleHidden(): Boolean {
        val next = !(filter.showHidden)
        filter = filter.copy(showHidden = next)
        prefs.showHidden = next
        _hiddenOn.value = next
        publish()
        return next
    }

    // ---------- 单文件操作（长按菜单，保留） ----------
    /**
     * 删除：默认进回收站（参照物 `l5` 反向证明默认行为）。
     * permanent=true 才真正抹掉。
     */
    fun delete(item: FileItem, permanent: Boolean = false) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (permanent) source.delete(item) else recycleBin.moveToTrash(item)
                }
                refreshTrashCount()
                open(pathStack.last())
            } catch (e: FileError) { _error.value = e.message }
            catch (e: Exception) { _error.value = e.message ?: "删除失败" }
        }
    }

    fun deleteAllSelected(items: List<FileItem>, permanent: Boolean = false) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    items.forEach {
                        if (permanent) source.delete(it) else recycleBin.moveToTrash(it)
                    }
                }
                refreshTrashCount()
                open(pathStack.last())
            } catch (e: FileError) { _error.value = e.message }
            catch (e: Exception) { _error.value = e.message ?: "删除失败" }
        }
    }

    // ---------- 回收站 ----------
    fun refreshTrashCount() {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { recycleBin.count() }
            _trashCount.value = n
        }
    }

    fun trashEntries(): List<RecycleBin.Entry> = recycleBin.list()
    fun restoreFromTrash(e: RecycleBin.Entry): Boolean =
        recycleBin.restore(e).also { if (it) refreshTrashCount() }
    fun deleteFromTrash(e: RecycleBin.Entry) =
        recycleBin.deleteForever(e).also { refreshTrashCount() }
    fun clearTrash() = recycleBin.clear().also { refreshTrashCount() }
    fun trashSize(): Long = recycleBin.totalSize()

    fun rename(item: FileItem, newName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { source.rename(item, newName) }
                open(pathStack.last())
            } catch (e: FileError) { _error.value = e.message }
            catch (e: Exception) { _error.value = e.message ?: "重命名失败" }
        }
    }

    fun mkdir(name: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { source.mkdir(pathStack.last(), name) }
                open(pathStack.last())
            } catch (e: FileError) { _error.value = e.message }
            catch (e: Exception) { _error.value = e.message ?: "新建失败" }
        }
    }

    fun currentPathValue(): String = pathStack.lastOrNull() ?: source.rootPath()
    fun rootPathValue(): String = source.rootPath()
    fun isPlainBrowse(): Boolean = _searchResults.value == null

    // ---------- 搜索（带范围） ----------
    fun search(keyword: String, scope: SearchScope) {
        viewModelScope.launch {
            _loading.value = true
            _searchScope.value = scope
            try {
                val r = withContext(Dispatchers.IO) {
                    source.search(currentPathValue(), keyword, scope)
                }
                _searchResults.value = r
            } catch (e: FileError) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "搜索失败"
                _searchResults.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    /** 深度搜索：从根目录搜（参照物 string ke 的 "Try deeper search"） */
    fun searchDeeper(keyword: String, scope: SearchScope) {
        viewModelScope.launch {
            _loading.value = true
            _searchScope.value = scope
            try {
                val r = withContext(Dispatchers.IO) {
                    source.search(source.rootPath(), keyword, scope, 500)
                }
                _searchResults.value = r
            } catch (e: FileError) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "搜索失败"
                _searchResults.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSearch() { _searchResults.value = null }
}
