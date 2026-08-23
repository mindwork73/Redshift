.class public final Lcom/example/service/SplitTunnelHelper;
.super Ljava/lang/Object;
.source "SplitTunnelHelper.java"


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static apply(Landroid/net/VpnService$Builder;Landroid/content/Context;)V
    .locals 5
    .param p0, "builder"    # Landroid/net/VpnService$Builder;
    .param p1, "context"    # Landroid/content/Context;

    if-eqz p0, :pswitch_done

    if-eqz p1, :pswitch_done

    :try_start_0
    invoke-virtual {p1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;

    move-result-object v0

    if-eqz v0, :pswitch_done

    new-instance v1, Ljava/io/File;

    const-string v2, "split_apps.txt"

    invoke-direct {v1, v0, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    new-instance v2, Ljava/io/BufferedReader;

    new-instance v3, Ljava/io/FileReader;

    invoke-direct {v3, v1}, Ljava/io/FileReader;-><init>(Ljava/io/File;)V

    invoke-direct {v2, v3}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    :pswitch_read
    invoke-virtual {v2}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v3

    if-eqz v3, :pswitch_close

    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v4

    if-nez v4, :pswitch_read

    const-string v4, "#"

    invoke-virtual {v3, v4}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-nez v4, :pswitch_read

    const-string v4, "exclude "

    invoke-virtual {v3, v4}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :pswitch_include

    :try_start_ex
    const/16 v4, 0x8

    invoke-virtual {v3, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {p0, v3}, Landroid/net/VpnService$Builder;->addDisallowedApplication(Ljava/lang/String;)Landroid/net/VpnService$Builder;
    :try_end_ex
    .catch Ljava/lang/Exception; {:try_start_ex .. :try_end_ex} :pswitch_catch_line

    goto/16 :pswitch_read

    :pswitch_include
    const-string v4, "include "

    invoke-virtual {v3, v4}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v4

    if-eqz v4, :pswitch_read

    :try_start_in
    const/16 v4, 0x8

    invoke-virtual {v3, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {p0, v3}, Landroid/net/VpnService$Builder;->addAllowedApplication(Ljava/lang/String;)Landroid/net/VpnService$Builder;
    :try_end_in
    .catch Ljava/lang/Exception; {:try_start_in .. :try_end_in} :pswitch_catch_line

    goto/16 :pswitch_read

    :pswitch_catch_line
    move-exception v0

    goto/16 :pswitch_read

    :pswitch_close
    invoke-virtual {v2}, Ljava/io/BufferedReader;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :pswitch_catch_cfg

    goto/16 :pswitch_done

    :pswitch_catch_cfg
    move-exception v0

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v1

    const-string v2, "RedShiftVPN"

    invoke-static {v2, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    :pswitch_done
    return-void
.end method
