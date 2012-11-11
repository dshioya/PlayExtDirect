Ext.define('Sample.view.Viewport', {

    requires: [
        'Sample.view.test.Panel'
    ],

    extend: 'Ext.container.Viewport',

    layout: 'fit',

    items: [
        {
            xtype: 'test_panel'
        }
    ]

});