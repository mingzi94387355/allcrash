在检测到一些模组时报错。

在模组运行后，会生成默认配置文件，在版本名\config\allcrash-common.toml
<img width="304" height="63" alt="image" src="https://github.com/user-attachments/assets/12af5f3b-9565-4875-9de6-8d6b6f61d1dc" />

上图为默认配置文件，可以把"examplemod"改为其他模组名字，使加载对应模组就崩溃，crash_mod为列表。
例如，使crash_mod = ["create","jei"]，则在游戏加载了机械动力，或JEI物品管理器时，报错。
