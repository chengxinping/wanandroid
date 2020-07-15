package com.xiaojianjun.wanandroid.ui.main.home.project

import androidx.lifecycle.MutableLiveData
import com.xiaojianjun.wanandroid.common.loadmore.LoadMoreStatus
import com.xiaojianjun.wanandroid.model.bean.Article
import com.xiaojianjun.wanandroid.model.bean.Category
import com.xiaojianjun.wanandroid.model.store.UserInfoStore
import com.xiaojianjun.wanandroid.model.store.isLogin
import com.xiaojianjun.wanandroid.base.BaseViewModel
import com.xiaojianjun.wanandroid.ui.common.CollectRepository
import com.xiaojianjun.wanandroid.common.bus.Bus
import com.xiaojianjun.wanandroid.common.bus.USER_COLLECT_UPDATED

class ProjectViewModel : BaseViewModel() {

    companion object {
        const val INITIAL_CHECKED = 0
        const val INITIAL_PAGE = 1
    }

    private val projectRepository by lazy { ProjectRepository() }
    private val collectRepository by lazy { CollectRepository() }

    val categories: MutableLiveData<MutableList<Category>> = MutableLiveData()
    val checkedCategory: MutableLiveData<Int> = MutableLiveData()
    val articleList: MutableLiveData<MutableList<Article>> = MutableLiveData()
    val loadMoreStatus = MutableLiveData<LoadMoreStatus>()
    val refreshStatus = MutableLiveData<Boolean>()
    val reloadStatus = MutableLiveData<Boolean>()
    val reloadListStatus = MutableLiveData<Boolean>()

    private var page = INITIAL_PAGE + 1


    fun getProjectCategory() {
        refreshStatus.value = true
        reloadStatus.value = false
        launch(
            block = {
                val categoryList = projectRepository.getProjectCategories()
                val checkedPosition = INITIAL_CHECKED
                val cid = categoryList[checkedPosition].id
                val pagination = projectRepository.getProjectListByCid(INITIAL_PAGE, cid)
                page = pagination.curPage

                categories.value = categoryList
                checkedCategory.value = checkedPosition
                articleList.value = pagination.datas.toMutableList()
                refreshStatus.value = false
            },
            error = {
                refreshStatus.value = false
                reloadStatus.value = true
            }
        )
    }

    fun refreshProjectList(checkedPosition: Int = checkedCategory.value ?: INITIAL_CHECKED) {
        refreshStatus.value = true
        reloadListStatus.value = false
        if (checkedPosition != checkedCategory.value) {
            articleList.value = mutableListOf()
            checkedCategory.value = checkedPosition
        }
        launch(
            block = {
                val categoryList = categories.value ?: return@launch
                val cid = categoryList[checkedPosition].id
                val pagination = projectRepository.getProjectListByCid(INITIAL_PAGE, cid)
                page = pagination.curPage

                articleList.value = pagination.datas.toMutableList()
                refreshStatus.value = false
            },
            error = {
                refreshStatus.value = false
                reloadListStatus.value = articleList.value?.isEmpty()
            }
        )
    }

    fun loadMoreProjectList() {
        loadMoreStatus.value = LoadMoreStatus.LOADING
        launch(
            block = {
                val categoryList = categories.value ?: return@launch
                val checkedPosition = checkedCategory.value ?: return@launch
                val cid = categoryList[checkedPosition].id
                val pagination = projectRepository.getProjectListByCid(page + 1, cid)
                page = pagination.curPage

                val currentList = articleList.value ?: mutableListOf()
                currentList.addAll(pagination.datas)

                articleList.value = currentList
                loadMoreStatus.value = if (pagination.offset >= pagination.total) {
                    LoadMoreStatus.END
                } else {
                    LoadMoreStatus.COMPLETED
                }
            },
            error = {
                loadMoreStatus.value = LoadMoreStatus.ERROR
            }
        )
    }

    fun collect(id: Int) {
        launch(
            block = {
                collectRepository.collect(id)
                UserInfoStore.addCollectId(id)
                updateItemCollectState(id to true)
                Bus.post(USER_COLLECT_UPDATED, id to true)
            },
            error = {
                updateItemCollectState(id to false)
            }
        )
    }

    fun uncollect(id: Int) {
        launch(
            block = {
                collectRepository.uncollect(id)
                UserInfoStore.removeCollectId(id)
                updateItemCollectState(id to false)
                Bus.post(USER_COLLECT_UPDATED, id to false)
            },
            error = {
                updateItemCollectState(id to true)
            }
        )
    }

    /**
     * 更新列表收藏状态
     */
    fun updateListCollectState() {
        val list = articleList.value
        if (list.isNullOrEmpty()) return
        if (isLogin()) {
            val collectIds = UserInfoStore.getUserInfo()?.collectIds ?: return
            list.forEach { it.collect = collectIds.contains(it.id) }
        } else {
            list.forEach { it.collect = false }
        }
        articleList.value = list
    }

    /**
     * 更新Item的收藏状态
     */
    fun updateItemCollectState(target: Pair<Int, Boolean>) {
        val list = articleList.value
        val item = list?.find { it.id == target.first } ?: return
        item.collect = target.second
        articleList.value = list
    }
}
