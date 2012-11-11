Ext.define('Sample.view.test.Panel', {

    requires: [
        'Sample.view.test.Form',
        'Sample.view.test.List'
    ],

    extend: 'Ext.panel.Panel',

    alias: 'widget.test_panel',

    items: [
        {
            itemId: 'push',
            xtype: 'button',
            text: 'push'
        },
        {
            xtype: 'test_form'
        },
        {
            xtype: 'test_list'
        }
    ]

});