Ext.define('Sample.view.test.List', {

    extend: 'Ext.grid.Panel',

    alias: 'widget.test_list',

    store: 'Tests',

    columns: [
        {header: 'name', dataIndex: 'name'}
    ]

});