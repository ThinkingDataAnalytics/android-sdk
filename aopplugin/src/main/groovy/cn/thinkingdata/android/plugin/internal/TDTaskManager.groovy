package cn.thinkingdata.android.plugin.internal

import cn.thinkingdata.android.plugin.internal.concurrent.BatchTaskScheduler

class TDTaskManager {
    ArrayList<File> aspectPath = new ArrayList<>()
    ArrayList<File> classPath = new ArrayList<>()
    List<String> ajcArgs = new ArrayList<>()
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    BatchTaskScheduler batchTaskScheduler = new BatchTaskScheduler()

    TDTaskManager() {
    }


    void addTask(TDTask task) {
        batchTaskScheduler.tasks << task
    }

    void batchExecute() {
        batchTaskScheduler.tasks.each { TDTask task ->
            task.encoding = encoding
            task.aspectPath = aspectPath
            task.classPath = classPath
            task.targetCompatibility = targetCompatibility
            task.sourceCompatibility = sourceCompatibility
            task.bootClassPath = bootClassPath
            task.ajcArgs = ajcArgs
        }

        batchTaskScheduler.execute()
    }
}
