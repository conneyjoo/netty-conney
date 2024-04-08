package com.conney.keeptriple.local.net.session;

public interface SessionListener {

    /**
     * 当设置session.id时触发created事件, 结束时event中的session属性被销毁
     *
     * @param event
     */
    void created(SessionEvent event);

    /**
     * 当调用session.active方法时触发active事件, 结束时event中的session属性被销毁
     *
     * @param event
     */
    void active(SessionEvent event);

    /**
     * 当调用session.change方法时触发change事件, 结束时event中的session属性被销毁
     *
     * @param event
     */
    void change(SessionEvent event);

    /**
     * 当调用session为无效时触发destroy事件, 结束时event中的session属性被销毁
     * 1.轮询检测所有session是否有效时可能被触发
     * 2.tcp连接断开调用close方法时触发
     * 3.调用expire时触发
     *
     * @param event
     */
    void destroy(SessionEvent event);
}
