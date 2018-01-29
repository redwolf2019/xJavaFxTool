package com.xwintop.xJavaFxTool.services.debugTools;

import com.alibaba.fastjson.JSON;
import com.xwintop.xJavaFxTool.controller.debugTools.ScriptEngineToolController;
import com.xwintop.xJavaFxTool.job.ScriptEngineToolJob;
import com.xwintop.xJavaFxTool.manager.ScheduleManager;
import com.xwintop.xJavaFxTool.manager.script.ScriptEngineManager;
import com.xwintop.xJavaFxTool.model.ScriptEngineToolTableBean;
import com.xwintop.xJavaFxTool.utils.ConfigureUtil;
import com.xwintop.xcore.util.javafx.AlertUtil;
import com.xwintop.xcore.util.javafx.FileChooserUtil;
import com.xwintop.xcore.util.javafx.TooltipUtil;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @ClassName: ScriptEngineToolService
 * @Description: 脚本引擎调试工具类
 * @author: xufeng
 * @date: 2018/1/28 22:59
 */

@Getter
@Setter
@Log4j
public class ScriptEngineToolService {
    private ScriptEngineToolController scriptEngineToolController;
    private String fileName = "scriptEngineToolConfigure.properties";
    private ScheduleManager scheduleManager = new ScheduleManager();
    public ScriptEngineToolService(ScriptEngineToolController scriptEngineToolController) {
        this.scriptEngineToolController = scriptEngineToolController;
    }

    /**
     * @Title: runAllAction
     * @Description: 运行所有动作
     */
    public void runAllAction() {
        for(ScriptEngineToolTableBean scriptEngineToolTableBean : scriptEngineToolController.getTableData()){
            if(scriptEngineToolTableBean.getIsEnabled()){
                runAction(scriptEngineToolTableBean);
            }
        }
    }

    /**
     * @Title: runAction
     * @Description: 单独运行
     */
    public void runAction(ScriptEngineToolTableBean scriptEngineToolTableBean) {
        String type = scriptEngineToolTableBean.getType();
        String script = scriptEngineToolTableBean.getScript();
        System.out.println("运行:" + type + " : " + script);
        Map parameterMap = null;
        if(StringUtils.isNotEmpty(scriptEngineToolTableBean.getParameter())){
            parameterMap = JSON.parseObject(scriptEngineToolTableBean.getParameter(),Map.class);
        }
        try {
            if (scriptEngineToolController.getTypeChoiceBoxStrings()[0].equals(type)) {// JavaScript脚本
                new ScriptEngineManager("JavaScript").exec(script,parameterMap);
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[1].equals(type)) {// JavaScript脚本文件
                new ScriptEngineManager("JavaScript").exec(new File(script));
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[2].equals(type)) {// Groovy脚本
                new ScriptEngineManager("Groovy").exec(script,parameterMap);
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[3].equals(type)) {// Groovy脚本文件
                new ScriptEngineManager("Groovy").exec(new File(script));
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[4].equals(type)) {// Python脚本
                new ScriptEngineManager("Python").exec(script,parameterMap);
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[5].equals(type)) {// Python脚本文件
                new ScriptEngineManager("Python").exec(new File(script));
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[6].equals(type)) {// Lua脚本
                new ScriptEngineManager("Lua").exec(script,parameterMap);
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[7].equals(type)) {// Lua脚本文件
                new ScriptEngineManager("Lua").exec(new File(script));
            }
            //继续执行后触发任务
            if(scriptEngineToolTableBean.getIsRunAfterActivate()){
                for(ScriptEngineToolTableBean tableBean:scriptEngineToolController.getTableData()){
                    if(tableBean.getOrder().equals(scriptEngineToolTableBean.getRunAfterActivate())){
                        runAction(tableBean);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * @Title: showScriptAction
     * @Description: 查看脚本文件
     */
    public void showScriptAction(ScriptEngineToolTableBean scriptEngineToolTableBean) {
        String type = scriptEngineToolTableBean.getType();
        String script = scriptEngineToolTableBean.getScript();
        System.out.println("查看:" + type + " : " + script);
        try {
            if (scriptEngineToolController.getTypeChoiceBoxStrings()[0].equals(type)) {// 命令行
                AlertUtil.showInfoAlert("脚本命令",script);
            } else if (scriptEngineToolController.getTypeChoiceBoxStrings()[1].equals(type)) {// 脚本文件
                Runtime.getRuntime().exec("NotePad.exe " + script);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    public boolean runQuartzAction(String quartzType, String cronText, int interval, int repeatCount) throws Exception {
        if ("简单表达式".equals(quartzType)) {
            scheduleManager.runQuartzAction(ScriptEngineToolJob.class,this,interval,repeatCount);
        } else if ("Cron表达式".equals(quartzType)) {
            if (StringUtils.isEmpty(cronText)) {
                TooltipUtil.showToast("cron表达式不能为空。");
                return false;
            }
            scheduleManager.runQuartzAction(ScriptEngineToolJob.class,this,cronText);
        }
        return true;
    }

    public boolean stopQuartzAction() throws Exception {
        scheduleManager.stopQuartzAction();
        return true;
    }

    public void saveConfigure() throws Exception {
        saveConfigure(ConfigureUtil.getConfigureFile(fileName));
    }

    public void saveConfigure(File file) throws Exception {
        FileUtils.touch(file);
        PropertiesConfiguration xmlConfigure = new PropertiesConfiguration(file);
        xmlConfigure.clear();
        for (int i = 0; i < scriptEngineToolController.getTableData().size(); i++) {
            xmlConfigure.setProperty("tableBean" + i, scriptEngineToolController.getTableData().get(i).getPropertys());
        }
        xmlConfigure.save();
        TooltipUtil.showToast("保存配置成功,保存在：" + file.getPath());
    }

    public void otherSaveConfigureAction() throws Exception {
        File file = FileChooserUtil.chooseSaveFile(fileName, new FileChooser.ExtensionFilter("All File", "*.*"),
                new FileChooser.ExtensionFilter("Properties", "*.properties"));
        if (file != null) {
            saveConfigure(file);
            TooltipUtil.showToast("保存配置成功,保存在：" + file.getPath());
        }
    }

    public void loadingConfigure() {
        loadingConfigure(ConfigureUtil.getConfigureFile(fileName));
    }

    public void loadingConfigure(File file) {
        try {
            scriptEngineToolController.getTableData().clear();
            PropertiesConfiguration xmlConfigure = new PropertiesConfiguration(file);
            xmlConfigure.getKeys().forEachRemaining(new Consumer<String>() {
                @Override
                public void accept(String t) {
                    scriptEngineToolController.getTableData().add(new ScriptEngineToolTableBean(xmlConfigure.getString(t)));
                }
            });
        } catch (Exception e) {
            try {
                log.error("加载配置失败：" + e.getMessage());
                TooltipUtil.showToast("加载配置失败：" + e.getMessage());
            } catch (Exception e2) {
            }
        }
    }

    public void loadingConfigureAction() {
        File file = FileChooserUtil.chooseFile(new FileChooser.ExtensionFilter("All File", "*.*"),
                new FileChooser.ExtensionFilter("Properties", "*.properties"));
        if (file != null) {
            loadingConfigure(file);
        }
    }
}