# Extract the SQL from MyBATIS XML files to clipboard.
      
Right-click inside a MyBATIS/iBATIS XML file's query tag (e.g. inside a "select") and click the "Extract Query to Clipboard" context menu item. The plugin will turn your mybatis statement into a real sql query (or, well, its best attempt) and load it into your clipboard.

![Right click in the tag you want extracted and select "Extract Query to Clipboard"][right-click-example-img]

## For example:

<tt>SqlExample.xml:</tt>

```xml
    [...] &lt;sql id="sometable"&gt;${prefix}Table&lt;/sql&gt;

    &lt;sql id="someinclude"&gt;
        from &lt;include refid="${include_target}"/&gt;
    &lt;/sql&gt;

    &lt;select id="selectWithIncludeProperties" resultType="map"&gt;
        select
        field1, field2, field3
        &lt;include refid="someinclude"&gt;
            &lt;property name="prefix" value="Some"/&gt;
            &lt;property name="include_target" value="sometable"/&gt;
        &lt;/include&gt;
    &lt;/select&gt; [...]
```

Output if you right-click within the "select" tag:

```sql
    select
    field1, field2, field3

    from SomeTable
```

Perfect for pasting into your favorite SQL IDE. Give or take. Anything it can't figure out automatically it will comment out. e.g. 


```sql
    select
    field1, field2, field3

    from SomeTable
    where
    /*&lt;foreach item="myItem" collection="myItems" open="(" close=")" separator=","&gt;
            #{myItem.someAttribute}
        &lt;/foreach&gt;
    */
```

This version is limited in functionality but is still very useful if you don't mind said limitations:

* Assumes you use iBATIS or MyBATIS XML file configuration
* Assumes the "&lt;sql&gt;" fragments are in the same file

[right-click-example-img]: https://github.com/inanutshellus/mybatis-xml-intellij-plugin/blob/master/mybatis-xml-intellij-plugin-right-click-example.png
