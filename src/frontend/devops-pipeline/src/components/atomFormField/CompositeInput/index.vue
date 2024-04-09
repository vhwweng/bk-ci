<template>
    <bk-input :disabled="disabled" :value="value" @change="handleChangeValue">
        <template v-if="prependText" slot="prepend">
        </template>
        <template v-if="appendText" slot="append">
        </template>
    </bk-input>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'composite-input',
        mixins: [atomFieldMixin],
        props: {
            value: String,
            appendText: String,
            prependText: String,
            disabled: Boolean
        },
        methods: {
            handleChangeValue (val) {
                const { name, handleChange } = this
                handleChange(name, val)
            }
        },
    }
</script>

<style lang="scss">
    
</style>
