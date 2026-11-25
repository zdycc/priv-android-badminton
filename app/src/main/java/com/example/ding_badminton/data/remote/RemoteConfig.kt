package com.example.ding_badminton.data.remote

import com.example.ding_badminton.BuildConfig

object RemoteConfig {
    const val BASE_URL = "http://www.api.zgyy.zjut.edu.cn"

    /**
     * 登录入口地址。CAS ticket 属于一次性凭证，不写进仓库；
     * 本地可在 local.properties 里配置 login.entryUrl，缺省打开 h5/home。
     */
    val LOGIN_ENTRY_URL: String = BuildConfig.LOGIN_ENTRY_URL
    const val LOGIN_SUCCESS_URL = "http://www.api.zgyy.zjut.edu.cn/h5/home"
    const val SUBMIT_ENDPOINT = "/staff.php/submitVenueOrderByStaff"
    const val CAMPUS_LIST_ENDPOINT = "/staff.php/getCampusLstByStaff"
    const val VENUE_LIST_ENDPOINT = "/staff.php/getVenueLstByStaff"
    const val LOGIN_CHECK_ENDPOINT = "/staff.php/getIndexByStaff"
}
