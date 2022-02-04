package cn.thinkingdata.android;

import static cn.thinkingdata.android.BuildConfig.VERSION_NAME;

public class TestProperties {

    /**
     * 是否必传：是
     * 描述：测试计划1，2，3，4...，测试计划唯一标识
     */
    public static int plan = 1;
    /**
     * 是否必传：是
     * 描述：平台 1 iOS； 2 Android； 3 JS； 4 小程序&小游戏
     */
    public static int platform = 2;
    /**
     * 是否必传：是
     * 描述：测试方式 1 单元测试 2 自动化测试  3 手动测试
     */
    public static int type = 1;
    /**
     * 是否必传：是
     * 描述：测试目标 1 功能测试 2.兼容性测试 3 健壮性测试 4 性能测试 5 压力测试 6 专项测试
     */
    public static int target = 1;
    /**
     * 是否必传：是
     * 描述：SDK版本号
     */
    public static String sdk_version = VERSION_NAME;
    /**
     * 是否必传：是
     * 描述：测试用例唯一标识，该值定义为TEST_XXXXX_X
     */
    String id = "";
    /**
     * 是否必传：是
     * 描述：测试用例名称
     */
    String name = "";
    /**
     * 是否必传：是
     * 描述：测试步骤
     */
    String step = "";
    /**
     * 是否必传：是
     * 描述：期望结果
     */
    String except = "";
    /**
     * 是否必传：是
     * 描述：测试结果
     */
    boolean result = false;
    /**
     * 是否必传：否
     * 描述：测试用例描述，备注等
     */
    String description = "";
    /**
     * 是否必传：否
     * 描述：输入参数，json字符串
     */
    String input = "";
    /**
     * 是否必传：否
     * 描述：输出参数
     */
    String output = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getExcept() {
        return except;
    }

    public void setExcept(String except) {
        this.except = except;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString() {

        return
                "{" +
                        "\"id\":\"" + id +
                        "\", \"name\":\"" + name +
                        "\", \"step\":\"" + step +
                        "\", \"except\":\"" + except +
                        "\", \"result\":" + result +
                        ", \"description\":\"" + description +
                        "\", \"input\":\"" + input +
                        "\", \"output\":\"" + output +
                        "\", \"sdk_version\":\"" + sdk_version +
                        "\", \"target\":" + target +
                        ", \"plan\":" + plan +
                        ", \"platform\":" + platform +
                        ", \"type\":" + type +
                        "}";
    }
}
