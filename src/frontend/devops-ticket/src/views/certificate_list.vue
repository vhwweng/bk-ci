<template>
    <article class="credential-certificate-content">
        <content-header>
            <template slot="left">
                <span class="inner-header-title">{{ $t('ticket.myCert') }}</span>
            </template>
        </content-header>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <div v-if="showContent && renderList.length" class="table-container">
                <bk-table
                    :data="renderList"
                    size="small"
                    :show-pagination-info="true"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageCountChange">
                    <bk-table-column :label="$t('ticket.name')" prop="certId"></bk-table-column>
                    <bk-table-column :label="$t('ticket.type')" prop="certType" :formatter="getShowType"></bk-table-column>
                    <bk-table-column :label="$t('ticket.cert.uploader')" prop="creator"></bk-table-column>
                    <bk-table-column :label="$t('ticket.cert.expireDate')" prop="expireTime" :formatter="convertToTime"></bk-table-column>
                    <bk-table-column :label="$t('ticket.remark')" prop="certRemark"></bk-table-column>
                    <bk-table-column :label="$t('ticket.operation')" width="200">
                        <template slot-scope="props">
                            <bk-button theme="primary" :class="{ 'cert-operation-btn': true, disabled: !hasPermission(props.row.permissions, 'edit') }" text @click="handleEditCert(props.row)">{{ $t('ticket.edit') }}</bk-button>
                            <bk-button theme="primary" :class="{ 'cert-operation-btn': true, disabled: !hasPermission(props.row.permissions, 'delete') }" text @click="handleDeleteCert(props.row)">{{ $t('ticket.delete') }}</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>

            <empty-tips :title="tip.title" :desc="tip.desc" :btns="tip.btns" v-if="showContent && !renderList.length"></empty-tips>
        </section>

    </article>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import emptyTips from '@/components/devops/emptyTips'

    export default {
        components: {
            emptyTips
        },
        data () {
            return {
                allowInit: false,
                loading: {
                    isLoading: false,
                    title: ''
                },
                pagination: {
                    count: 0,
                    limit: 10,
                    current: 1
                },
                showContent: false,
                certList: [],
                renderList: [],
                tip: {
                    title: this.$t('ticket.cert.emptyCert'),
                    desc: this.$t('ticket.cert.emptyCertTips'),
                    btns: [
                        {
                            text: this.$t('ticket.createCert'),
                            type: 'primary',
                            size: 'normal',
                            handler: this.addCertificateHandler
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
                await this.init()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            convertToTime (row, cell, time) {
                return convertTime(time * 1000)
            },
            getShowType (row, cell, type) {
                switch (type) {
                    case 'android':
                        return 'Android'
                    case 'ios':
                        return 'iOS'
                    case 'tls':
                        return 'SSL/TLS'
                    case 'enterprise':
                        return this.$t('ticket.cert.iosCorporatesignCert')
                    default:
                        return type
                }
            },
            updateList () {
                const { limit, current } = this.pagination
                this.renderList.splice(0, this.renderList.length, ...this.certList.slice((current - 1) * limit, current * limit))
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.updateList()
            },
            handlePageCountChange (limit) {
                this.pagination.limit = limit
                if (this.pagination.current * limit > this.certList.length) {
                    this.pagination.current = 1
                }
                this.updateList()
            },
            async init () {
                this.loading.isLoading = true
                this.loading.title = this.$t('ticket.loadingTitle')

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                    console.error(this.$t('ticket.cert.getCertError'))
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 300)
                }
            },
            async requestList () {
                try {
                    const res = await this.$store.dispatch('ticket/requestCertificateList', {
                        projectId: this.projectId,
                        page: 1,
                        pageSize: 1000
                    })
                    this.certList.splice(0, this.renderList.length, ...res.records)
                    this.updateList()
                    this.pagination.count = res.count
                } catch (err) {
                    if (err.code === 403) {
                        this.certList.splice(0, this.certList.length)
                        this.updateList()
                        this.showContent = true
                        return
                    }
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
                this.showContent = true
            },
            handleEditCert (cert) {
                if (this.hasPermission(cert.permissions, 'edit')) {
                    this.$router.push({
                        name: 'editCert',
                        params: {
                            certType: cert.certType,
                            certId: cert.certId
                        }
                    })
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.cert,
                            instanceId: [{
                                id: cert.certId,
                                name: cert.certId
                            }],
                            projectId: this.projectId
                        }]
                    })
                }
            },
            hasPermission (permissions, action) {
                return permissions && permissions[action]
            },
            async handleDeleteCert (cert) {
                if (this.hasPermission(cert.permissions, 'delete')) {
                    this.$bkInfo({
                        theme: 'warning',
                        type: 'warning',
                        title: `${this.$t('ticket.cert.deleteCertTips', [cert.certId])}?`,
                        confirmFn: async () => {
                            let message, theme
                            try {
                                await this.$store.dispatch('ticket/toDeleteCerts', {
                                    projectId: this.projectId,
                                    id: cert.certId
                                })
                                message = this.$t('ticket.cert.successfullyDeletedCert')
                                theme = 'success'
                            } catch (err) {
                                message = err.data ? err.data.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })
                                this.requestList()
                            }
                        }
                    })
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.delete,
                            resourceId: this.$permissionResourceMap.cert,
                            instanceId: [{
                                id: cert.certId,
                                name: cert.certId
                            }],
                            projectId: this.projectId
                        }]
                    })
                }
            },
            addCertificateHandler () {
                this.$router.push('createCert')
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';
    .cert-operation-btn {
      &.disabled {
        color: #C4C6CC;
        &:hover {
          color: #C4C6CC;
        }
        cursor: url(../images/cursor-lock.png),auto !important;
      }
    }
</style>
