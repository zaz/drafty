import * as shell from "shelljs";

shell.cp("-R", "src/public/js/fuse/", "dist/public/js/");
shell.cp("-R", "src/public/fonts", "dist/public/");
shell.cp("-R", "src/public/webfonts", "dist/public/");
shell.cp("-R", "src/public/images", "dist/public/");
