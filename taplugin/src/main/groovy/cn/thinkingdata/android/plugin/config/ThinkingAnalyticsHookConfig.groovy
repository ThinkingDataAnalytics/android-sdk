package cn.thinkingdata.android.plugin.config

import cn.thinkingdata.android.plugin.entity.ThinkingAnalyticsMethodCell
import org.objectweb.asm.Opcodes

class ThinkingAnalyticsHookConfig {
    public static final String THINKING_ANALYTICS_API = "cn/thinkingdata/android/aop/ThinkingDataAutoTrackHelper"
    public final static HashMap<String, ThinkingAnalyticsMethodCell> INTERFACE_METHODS = new HashMap<>()
    public final static HashMap<String, ThinkingAnalyticsMethodCell> CLASS_METHODS = new HashMap<>()

    static {
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onCheckedChanged',
                '(Landroid/widget/CompoundButton;Z)V',
                'android/widget/CompoundButton$OnCheckedChangeListener',
                'trackViewOnClick',
                '(Landroid/view/View;Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onRatingChanged',
                '(Landroid/widget/RatingBar;FZ)V',
                'android/widget/RatingBar$OnRatingBarChangeListener',
                'trackViewOnClick',
                '(Landroid/view/View;Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onStopTrackingTouch',
                '(Landroid/widget/SeekBar;)V',
                'android/widget/SeekBar$OnSeekBarChangeListener',
                'trackViewOnClick',
                '(Landroid/view/View;Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onCheckedChanged',
                '(Landroid/widget/RadioGroup;I)V',
                'android/widget/RadioGroup$OnCheckedChangeListener',
                'trackRadioGroup',
                '(Landroid/widget/RadioGroup;ILjava/lang/String;)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/content/DialogInterface;I)V',
                'android/content/DialogInterface$OnClickListener',
                'trackDialog',
                '(Landroid/content/DialogInterface;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onItemSelected',
                '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V',
                'android/widget/AdapterView$OnItemSelectedListener',
                'trackListView',
                '(Landroid/widget/AdapterView;Landroid/view/View;I)V',
                1, 3,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onGroupClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z',
                'android/widget/ExpandableListView$OnGroupClickListener',
                'trackExpandableListViewOnGroupClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;I)V',
                1, 3,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onChildClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z',
                'android/widget/ExpandableListView$OnChildClickListener',
                'trackExpandableListViewOnChildClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;II)V',
                1, 4,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onTabChanged',
                '(Ljava/lang/String;)V',
                'android/widget/TabHost$OnTabChangeListener',
                'trackTabHost',
                '(Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onTabSelected',
                '(Landroid/support/design/widget/TabLayout$Tab;)V',
                'android/support/design/widget/TabLayout$OnTabSelectedListener',
                'trackTabLayoutSelected',
                '(Ljava/lang/Object;Ljava/lang/Object;)V',
                0, 2,
                [Opcodes.ALOAD, Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onTabSelected',
                '(Lcom/google/android/material/tabs/TabLayout$Tab;)V',
                'com/google/android/material/tabs/TabLayout$OnTabSelectedListener',
                'trackTabLayoutSelected',
                '(Ljava/lang/Object;Ljava/lang/Object;)V',
                0, 2,
                [Opcodes.ALOAD, Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'android/widget/Toolbar$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'android/support/v7/widget/Toolbar$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'androidx/appcompat/widget/Toolbar$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/content/DialogInterface;IZ)V',
                'android/content/DialogInterface$OnMultiChoiceClickListener',
                'trackDialog',
                '(Landroid/content/DialogInterface;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'android/widget/PopupMenu$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'androidx/appcompat/widget/PopupMenu$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'android/support/v7/widget/PopupMenu$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'com/google/android/material/navigation/NavigationView$OnNavigationItemSelectedListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'android/support/design/widget/NavigationView$OnNavigationItemSelectedListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'android/support/design/widget/BottomNavigationView$OnNavigationItemSelectedListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'com/google/android/material/bottomnavigation/BottomNavigationView$OnNavigationItemSelectedListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))

        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'android/view/MenuItem$OnMenuItemClickListener',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onTimeSet',
                '(Landroid/widget/TimePicker;II)V',
                'android/app/TimePickerDialog$OnTimeSetListener',
                'trackViewOnClick',
                '(Landroid/view/View;Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addInterfaceMethod(new ThinkingAnalyticsMethodCell(
                'onDateSet',
                '(Landroid/widget/DatePicker;III)V',
                'android/app/DatePickerDialog$OnDateSetListener',
                'trackViewOnClick',
                '(Landroid/view/View;Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
    }

    static {
        addClassMethod(new ThinkingAnalyticsMethodCell(
                'performClick',
                '()Z',
                'androidx/appcompat/widget/ActionMenuPresenter$OverflowMenuButton',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                0, 1,
                [Opcodes.ALOAD]))

        addClassMethod(new ThinkingAnalyticsMethodCell(
                'performClick',
                '()Z',
                'android/support/v7/widget/ActionMenuPresenter$OverflowMenuButton',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                0, 1,
                [Opcodes.ALOAD]))

        addClassMethod(new ThinkingAnalyticsMethodCell(
                'performClick',
                '()Z',
                'android/widget/ActionMenuPresenter$OverflowMenuButton',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                0, 1,
                [Opcodes.ALOAD]))
    }

    static void addInterfaceMethod(ThinkingAnalyticsMethodCell sensorsAnalyticsMethodCell) {
        if (sensorsAnalyticsMethodCell != null) {
            INTERFACE_METHODS.put(sensorsAnalyticsMethodCell.parent + sensorsAnalyticsMethodCell.name + sensorsAnalyticsMethodCell.desc, sensorsAnalyticsMethodCell)
        }
    }

    static void addClassMethod(ThinkingAnalyticsMethodCell sensorsAnalyticsMethodCell) {
        if (sensorsAnalyticsMethodCell != null) {
            CLASS_METHODS.put(sensorsAnalyticsMethodCell.parent + sensorsAnalyticsMethodCell.name + sensorsAnalyticsMethodCell.desc, sensorsAnalyticsMethodCell)
        }
    }

    /**
     * android.gradle 3.2.1 版本中，针对 Lambda 表达式处理
     */

    public final static HashMap<String, ThinkingAnalyticsMethodCell> LAMBDA_METHODS = new HashMap<>()
    //lambda 参数优化取样
    public final static ArrayList<ThinkingAnalyticsMethodCell> SAMPLING_LAMBDA_METHODS = new ArrayList<>()
    static {
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/view/View;)V',
                'Landroid/view/View$OnClickListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))
        SAMPLING_LAMBDA_METHODS.add(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/view/View;)V',
                'Landroid/view/View$OnClickListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onCheckedChanged',
                '(Landroid/widget/CompoundButton;Z)V',
                'Landroid/widget/CompoundButton$OnCheckedChangeListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onRatingChanged',
                '(Landroid/widget/RatingBar;FZ)V',
                'Landroid/widget/RatingBar$OnRatingBarChangeListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onCheckedChanged',
                '(Landroid/widget/RadioGroup;I)V',
                'Landroid/widget/RadioGroup$OnCheckedChangeListener;',
                'trackRadioGroup',
                '(Landroid/widget/RadioGroup;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        SAMPLING_LAMBDA_METHODS.add(new ThinkingAnalyticsMethodCell(
                'onCheckedChanged',
                '(Landroid/widget/RadioGroup;I)V',
                'Landroid/widget/RadioGroup$OnCheckedChangeListener;',
                'trackRadioGroup',
                '(Landroid/widget/RadioGroup;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/content/DialogInterface;I)V',
                'Landroid/content/DialogInterface$OnClickListener;',
                'trackDialog',
                '(Landroid/content/DialogInterface;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onItemClick',
                '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V',
                'Landroid/widget/AdapterView$OnItemClickListener;',
                'trackListView',
                '(Landroid/widget/AdapterView;Landroid/view/View;I)V',
                1, 3,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD]))
        SAMPLING_LAMBDA_METHODS.add(new ThinkingAnalyticsMethodCell(
                'onItemClick',
                '(Landroid/widget/AdapterView;Landroid/view/View;IJ)V',
                'Landroid/widget/AdapterView$OnItemClickListener;',
                'trackListView',
                '(Landroid/widget/AdapterView;Landroid/view/View;I)V',
                1, 3,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onGroupClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;IJ)Z',
                'Landroid/widget/ExpandableListView$OnGroupClickListener;',
                'trackExpandableListViewOnGroupClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;I)V',
                1, 3,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onChildClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;IIJ)Z',
                'Landroid/widget/ExpandableListView$OnChildClickListener;',
                'trackExpandableListViewOnChildClick',
                '(Landroid/widget/ExpandableListView;Landroid/view/View;II)V',
                1, 4,
                [Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onTabChanged',
                '(Ljava/lang/String;)V',
                'Landroid/widget/TabHost$OnTabChangeListener;',
                'trackTabHost',
                '(Ljava/lang/String;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'Lcom/google/android/material/navigation/NavigationView$OnNavigationItemSelectedListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/support/design/widget/NavigationView$OnNavigationItemSelectedListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/support/design/widget/BottomNavigationView$OnNavigationItemSelectedListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onNavigationItemSelected',
                '(Landroid/view/MenuItem;)Z',
                'Lcom/google/android/material/bottomnavigation/BottomNavigationView$OnNavigationItemSelectedListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/widget/Toolbar$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/support/v7/widget/Toolbar$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroidx/appcompat/widget/Toolbar$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onClick',
                '(Landroid/content/DialogInterface;IZ)V',
                'Landroid/content/DialogInterface$OnMultiChoiceClickListener;',
                'trackDialog',
                '(Landroid/content/DialogInterface;I)V',
                1, 2,
                [Opcodes.ALOAD, Opcodes.ILOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/widget/PopupMenu$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroidx/appcompat/widget/PopupMenu$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))
        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/support/v7/widget/PopupMenu$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))

        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onMenuItemClick',
                '(Landroid/view/MenuItem;)Z',
                'Landroid/view/MenuItem$OnMenuItemClickListener;',
                'trackMenuItem',
                '(Landroid/view/MenuItem;)V',
                1, 1,
                [Opcodes.ALOAD]))

        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onTimeSet',
                '(Landroid/widget/TimePicker;II)V',
                'Landroid/app/TimePickerDialog$OnTimeSetListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))

        addLambdaMethod(new ThinkingAnalyticsMethodCell(
                'onDateSet',
                '(Landroid/widget/DatePicker;III)V',
                'Landroid/app/DatePickerDialog$OnDateSetListener;',
                'trackViewOnClick',
                '(Landroid/view/View;)V',
                1, 1,
                [Opcodes.ALOAD]))

        // Todo: 扩展
    }

    static void addLambdaMethod(ThinkingAnalyticsMethodCell sensorsAnalyticsMethodCell) {
        if (sensorsAnalyticsMethodCell != null) {
            LAMBDA_METHODS.put(sensorsAnalyticsMethodCell.parent + sensorsAnalyticsMethodCell.name + sensorsAnalyticsMethodCell.desc, sensorsAnalyticsMethodCell)
        }
    }
}