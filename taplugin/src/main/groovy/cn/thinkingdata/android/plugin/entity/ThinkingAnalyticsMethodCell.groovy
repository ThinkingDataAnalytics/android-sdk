package cn.thinkingdata.android.plugin.entity
/**
 * 方法属性封装
 */
class ThinkingAnalyticsMethodCell {
    /**
     * 方法名
     */
    String name
    /**
     * 方法描述
     */
    String desc
    /**
     * 接口或类
     */
    String parent
    /**
     * 发送数据的方法名
     */
    String agentName
    /**
     * 发送数据的方法描述
     */
    String agentDesc
    /**
     * 发送数据的方法参数起始索引
     */
    int paramsStart
    /**
     * 发送数据的方法参数个数
     */
    int paramsCount

    List<Integer> opcodes

    ThinkingAnalyticsMethodCell(String name, String desc, String agentName) {
        this.name = name
        this.desc = desc
        this.agentName = agentName
    }

    ThinkingAnalyticsMethodCell(String name, String desc, String parent, String agentName, String agentDesc, int paramsStart, int paramsCount, List<Integer> opcodes) {
        this.name = name
        this.desc = desc
        this.parent = parent
        this.agentName = agentName
        this.agentDesc = agentDesc
        this.paramsStart = paramsStart
        this.paramsCount = paramsCount
        this.opcodes = opcodes
    }

    @Override
    boolean equals(Object cell) {
        return this.name == cell.name && this.desc == cell.desc && this.parent == cell.parent
    }
}