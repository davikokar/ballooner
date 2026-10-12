# Add project specific ProGuard rules here.

# Balloon types and fonts are stored in the database as the constant's own name and read back by
# matching it, so R8 must not rename them.
-keepclassmembers enum com.ballooner.** {
    <fields>;
}
