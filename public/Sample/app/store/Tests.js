Ext.define('Sample.store.Tests', {

    requires: [
        'Sample.model.Test'
    ],

    extend: 'Ext.data.Store',

    model: 'Sample.model.Test',

    storeId: 'Tests',

    autoLoad: true,

    proxy: {
        type  : 'direct',

        api: {
            read: Sample.direct.Test.search
        },

        paramOrder: [
            'page',
            'start',
            'limit'
        ],

        reader: {
            type: 'json',
            root: 'items'
        }
    }

});