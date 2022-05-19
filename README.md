# android_replay_test
android项目。

需求：
实现【测试程序】记录【目标程序】的一系列操作，并能够回放这些操作。 

---

**目标程序**：比较简单，主要就是几个组件的编写。

**测试程序**：在不改动 目标 程序代码的情况下，使用**HOOK**技术，完成需求。

---
**环境**：夜神模拟器7.0.2.7
**额外模块**：Xposed，需要使用到ROOT权限。

**思路**：

（1）HOOK **目标程序** 的组件，使得我们点击组件后，能够**广播**信息到 **测试程序**，并保存起来。

（2）同时，为 **目标程序** 添加 **接收广播类**。

（3）回放的时候，我们将保存的信息 **广播** 给 **目标程序**，由 **接收广播类**，“回放” 动作。

> **为什么要使用广播？**
> 
> 答：广播是跨程序通信方式之一。当前其他方式也可以，只是广播比较简单而已。
> 
> **Xposed模块可以传递变量吗？**
> 
> 答：Xposed模块在hook时（package加载时），存在于不同的进程中。即使 定义**static**属性，也无法跨进程传递。
> 
> **使用createPackageContext**获得 目标程序 Context，再获得View不行吗？
> 
> 答：确实不行，获得Context不是Activity，无法获得View。（目前没有找到方法）

> 还有，Xposed在某个类中，无法“添加”一个属性。只能获取、修改 已定义的属性。